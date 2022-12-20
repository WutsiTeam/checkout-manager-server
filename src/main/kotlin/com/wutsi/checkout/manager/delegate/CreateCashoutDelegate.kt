package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.CreateCashoutRequest
import com.wutsi.checkout.manager.dto.CreateCashoutResponse
import com.wutsi.checkout.manager.workflow.CreateCashoutWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CreateCashoutDelegate(private val workflow: CreateCashoutWorkflow) {
    public fun invoke(request: CreateCashoutRequest): CreateCashoutResponse =
        workflow.execute(request, WorkflowContext())
}
