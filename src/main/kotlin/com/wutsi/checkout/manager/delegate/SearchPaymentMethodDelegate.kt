package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.SearchPaymentMethodRequest
import com.wutsi.checkout.manager.dto.SearchPaymentMethodResponse
import com.wutsi.checkout.manager.workflow.SearchPaymentMethodWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchPaymentMethodDelegate(private val workflow: SearchPaymentMethodWorkflow) {
    public fun invoke(request: SearchPaymentMethodRequest): SearchPaymentMethodResponse =
        workflow.execute(request, WorkflowContext())
}
