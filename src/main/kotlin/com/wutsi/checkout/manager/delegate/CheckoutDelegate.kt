package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.CheckoutRequest
import com.wutsi.checkout.manager.dto.CheckoutResponse
import com.wutsi.checkout.manager.workflow.CheckoutWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CheckoutDelegate(private val workflow: CheckoutWorkflow) {
    public fun invoke(request: CheckoutRequest): CheckoutResponse =
        workflow.execute(request, WorkflowContext())
}
