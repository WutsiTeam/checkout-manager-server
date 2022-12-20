package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.GetOrderResponse
import com.wutsi.checkout.manager.workflow.GetOrderWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class GetOrderDelegate(private val workflow: GetOrderWorkflow) {
    public fun invoke(id: String): GetOrderResponse =
        workflow.execute(id, WorkflowContext())
}
