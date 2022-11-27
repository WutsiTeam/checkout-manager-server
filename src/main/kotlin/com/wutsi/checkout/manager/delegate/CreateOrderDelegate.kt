package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import com.wutsi.checkout.manager.workflow.CreateOrderWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CreateOrderDelegate(private val workflow: CreateOrderWorkflow) {
    public fun invoke(request: CreateOrderRequest): CreateOrderResponse =
        workflow.execute(request, WorkflowContext())
}
