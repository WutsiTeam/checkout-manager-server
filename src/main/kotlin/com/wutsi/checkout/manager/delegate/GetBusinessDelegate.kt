package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.GetBusinessResponse
import com.wutsi.checkout.manager.workflow.GetBusinessWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class GetBusinessDelegate(private val workflow: GetBusinessWorkflow) {
    public fun invoke(id: Long): GetBusinessResponse =
        workflow.execute(id, WorkflowContext())
}
