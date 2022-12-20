package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.PaymentProviderSummary
import com.wutsi.checkout.manager.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.manager.dto.SearchPaymentProviderResponse
import com.wutsi.event.PaymentMethodEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchPaymentProviderWorkflow(
    eventStream: EventStream,
) : AbstractPaymentMethodWorkflow<SearchPaymentProviderRequest, SearchPaymentProviderResponse>(eventStream) {
    override fun getEventType(
        request: SearchPaymentProviderRequest,
        response: SearchPaymentProviderResponse,
        context: WorkflowContext,
    ): String? = null

    override fun toEventPayload(
        request: SearchPaymentProviderRequest,
        response: SearchPaymentProviderResponse,
        context: WorkflowContext,
    ): PaymentMethodEventPayload? = null

    override fun getValidationRules(request: SearchPaymentProviderRequest, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(
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
