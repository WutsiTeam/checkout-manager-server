package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.manager.dto.SearchPaymentProviderResponse
import com.wutsi.checkout.manager.workflow.SearchPaymentProviderWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchPaymentProviderDelegate(
    private val logger: KVLogger,
    private val workflow: SearchPaymentProviderWorkflow,
) {
    public fun invoke(request: SearchPaymentProviderRequest): SearchPaymentProviderResponse {
        logger.add("request_country", request.country)
        logger.add("request_number", request.number)
        logger.add("request_type", request.type)

        val response = workflow.execute(request, WorkflowContext())
        logger.add("count", response.paymentProviders.size)

        return response
    }
}
