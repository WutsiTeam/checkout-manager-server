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
class CompleteOrderTask(
    private val checkoutAccessApi: CheckoutAccessApi,
    private val workflowEngine: WorkflowEngine,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "complete-order")
        const val CONTEXT_ORDER_ID = "order-id"
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        checkoutAccessApi.updateOrderStatus(
            id = context.data[CONTEXT_ORDER_ID] as String,
            request = UpdateOrderStatusRequest(
                status = OrderStatus.COMPLETED.name,
            ),
        )
    }
}
