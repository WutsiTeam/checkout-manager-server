package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class CompleteTransactionWorkflow(
    eventStream: EventStream,
    private val logger: KVLogger
) : AbstractCheckoutWorkflow<String, Unit, Void?>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(transactionId: String, response: Unit, context: WorkflowContext): Void? = null

    override fun getValidationRules(transactionId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(transactionId: String, context: WorkflowContext) {
        // Get Transaction
        val tx = checkoutAccessApi.getTransaction(transactionId).transaction

        // Update order
        logger.add("order_id", tx.orderId)
        if (tx.orderId != null) {
            val order = checkoutAccessApi.getOrder(tx.orderId!!).order
            logger.add("order_status", order.status)
            if (order.status != OrderStatus.OPENED.name) {
                openOrder(tx.orderId!!)
                logger.add("order_opened", true)
            }
        }
    }

    private fun openOrder(orderId: String): Boolean {
        val order = checkoutAccessApi.getOrder(orderId).order
        if (order.status != OrderStatus.OPENED.name) {
            checkoutAccessApi.updateOrderStatus(
                id = orderId,
                request = UpdateOrderStatusRequest(
                    status = OrderStatus.OPENED.name
                )
            )
            return true
        } else {
            return false
        }
    }
}
