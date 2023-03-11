package com.wutsi.checkout.manager.workflow.task

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class ExpireOrderTask(
    private val workflowEngine: WorkflowEngine,
    private val checkoutAccessApi: CheckoutAccessApi,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "expire-order")
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        val orderId = context.input as String

        expire(orderId)
        cancelReservation(orderId, context)
    }

    private fun expire(orderId: String) =
        checkoutAccessApi.updateOrderStatus(orderId, UpdateOrderStatusRequest(OrderStatus.EXPIRED.name))

    private fun cancelReservation(orderId: String, context: WorkflowContext) =
        workflowEngine.executeAsync(
            CancelOrderReservationTask.ID,
            WorkflowContext(
                accountId = context.accountId,
                data = mutableMapOf(
                    CancelOrderReservationTask.CONTEXT_ORDER_ID to orderId,
                ),
            ),
        )
}
