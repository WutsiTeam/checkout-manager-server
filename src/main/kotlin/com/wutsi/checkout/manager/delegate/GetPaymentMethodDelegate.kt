package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.GetPaymentMethodResponse
import com.wutsi.checkout.manager.workflow.GetPaymentMethodWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class GetPaymentMethodDelegate(private val workflow: GetPaymentMethodWorkflow) {
    public fun invoke(token: String): GetPaymentMethodResponse =
        workflow.execute(token, WorkflowContext())
}
