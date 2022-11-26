package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.manager.dto.SearchPaymentProviderResponse
import com.wutsi.checkout.manager.workflow.SearchPaymentProviderWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchPaymentProviderDelegate(private val workflow: SearchPaymentProviderWorkflow) {
    public fun invoke(request: SearchPaymentProviderRequest): SearchPaymentProviderResponse =
        workflow.execute(request, WorkflowContext())
}
