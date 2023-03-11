package com.wutsi.checkout.manager.workflow.task

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.access.dto.Transaction
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.TransactionType
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class HandleSuccessfulTransactionTask(
    private val workflowEngine: WorkflowEngine,
    private val checkoutAccessApi: CheckoutAccessApi,
    private val logger: KVLogger,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "handle-successful-transaction")
        const val CONTEXT_TRANSACTION_ID = "transaction-id"
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        val transactionId = context.data[CONTEXT_TRANSACTION_ID] as String
        val tx = checkoutAccessApi.getTransaction(transactionId).transaction
        if (tx.status != Status.SUCCESSFUL.name) { // Just in case
            return
        }

        when (tx.type) {
            TransactionType.CHARGE.name -> handleSuccessfulCharge(tx, context)
            else -> {}
        }
    }

    private fun handleSuccessfulCharge(tx: Transaction, context: WorkflowContext) {
        // Get the order
        val orderId = tx.orderId!!
        val order = checkoutAccessApi.getOrder(orderId).order
        logger.add("order_status", order.status)
        if (order.status == OrderStatus.IN_PROGRESS.name) { // Already processed
            return
        }

        // Open the order
        checkoutAccessApi.updateOrderStatus(
            id = orderId,
            request = UpdateOrderStatusRequest(
                status = OrderStatus.IN_PROGRESS.name,
            ),
        )

        sendOrderToCustomer(order, context)
        sendOrderToMerchant(order, context)
    }

    private fun sendOrderToCustomer(order: Order, context: WorkflowContext) =
        workflowEngine.executeAsync(
            SendOrderToCustomerTask.ID,
            context.copy(
                data = mutableMapOf(
                    AbstractSendOrderTask.CONTEXT_ORDER_ID to order.id,
                ),
            ),
        )

    private fun sendOrderToMerchant(order: Order, context: WorkflowContext) =
        workflowEngine.executeAsync(
            SendOrderToMerchantTask.ID,
            context.copy(
                data = mutableMapOf(
                    AbstractSendOrderTask.CONTEXT_ORDER_ID to order.id,
                ),
            ),
        )
}
