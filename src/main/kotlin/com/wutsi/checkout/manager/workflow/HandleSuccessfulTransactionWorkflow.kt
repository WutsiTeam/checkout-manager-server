package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.access.dto.Transaction
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.TransactionType
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class HandleSuccessfulTransactionWorkflow(
    eventStream: EventStream,
    private val logger: KVLogger
) : AbstractCheckoutWorkflow<String, Unit, Void?>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(transactionId: String, response: Unit, context: WorkflowContext): Void? = null

    override fun getValidationRules(transactionId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(transactionId: String, context: WorkflowContext) {
        val tx = checkoutAccessApi.getTransaction(transactionId).transaction
        if (tx.status != Status.SUCCESSFUL.name) { // Just in case
            return
        }

        when (tx.type) {
            TransactionType.CHARGE.name -> handleCharge(tx)
            else -> {}
        }
    }

    private fun handleCharge(tx: Transaction) {
        // Get the order
        val orderId = tx.orderId!!
        val order = checkoutAccessApi.getOrder(orderId).order
        logger.add("order_status", order.status)
        if (order.status == OrderStatus.OPENED.name) { // Already opened
            return
        }

        // Open the order
        checkoutAccessApi.updateOrderStatus(
            id = orderId,
            request = UpdateOrderStatusRequest(
                status = OrderStatus.OPENED.name
            )
        )

        sendOrderToCustomer(order)
        sendOrderToMerchant(order)
    }

    private fun sendOrderToCustomer(order: Order) {
        try {
            eventStream.enqueue(InternalEventURN.ORDER_TO_CUSTOMER_SUBMITTED.urn, OrderEventPayload(orderId = order.id))
        } catch (ex: Exception) {
            // Ignore
        }
    }

    private fun sendOrderToMerchant(order: Order) {
        try {
            eventStream.enqueue(InternalEventURN.ORDER_TO_MERCHANT_SUBMITTED.urn, OrderEventPayload(orderId = order.id))
        } catch (ex: Exception) {
            // Ignore
        }
    }
}
