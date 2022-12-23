package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.PaymentProviderSummary
import com.wutsi.checkout.manager.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.manager.dto.SearchPaymentProviderResponse
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchPaymentProviderWorkflow :
    AbstractQueryWorkflow<SearchPaymentProviderRequest, SearchPaymentProviderResponse>() {
    override fun execute(
        request: SearchPaymentProviderRequest,
        context: WorkflowContext,
    ): SearchPaymentProviderResponse {
        val providers = checkoutAccessApi.searchPaymentProvider(
            request = com.wutsi.checkout.access.dto.SearchPaymentProviderRequest(
                number = request.number,
                country = request.country,
                type = request.type,
            ),
        ).paymentProviders
        return SearchPaymentProviderResponse(
            paymentProviders = providers.map {
                PaymentProviderSummary(
                    id = it.id,
                    name = it.name,
                    logoUrl = it.logoUrl,
                    code = it.code,
                    type = it.type,
                )
            },
        )
    }
}
