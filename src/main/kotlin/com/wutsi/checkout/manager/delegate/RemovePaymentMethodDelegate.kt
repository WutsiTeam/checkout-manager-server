package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.workflow.RemovePaymentMethodWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class RemovePaymentMethodDelegate(private val workflow: RemovePaymentMethodWorkflow) {
    public fun invoke(token: String) {
        workflow.execute(token, WorkflowContext())
    }
}
