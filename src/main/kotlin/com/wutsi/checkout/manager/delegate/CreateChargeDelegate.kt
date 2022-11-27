package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.CreateChargeRequest
import com.wutsi.checkout.manager.dto.CreateChargeResponse
import com.wutsi.checkout.manager.workflow.CreateChargeWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CreateChargeDelegate(private val workflow: CreateChargeWorkflow) {
    public fun invoke(request: CreateChargeRequest): CreateChargeResponse =
        workflow.execute(request, WorkflowContext())
}
