package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.PaymentMethodSummary
import com.wutsi.checkout.manager.dto.PaymentProviderSummary
import com.wutsi.checkout.manager.dto.SearchPaymentMethodRequest
import com.wutsi.checkout.manager.dto.SearchPaymentMethodResponse
import com.wutsi.checkout.manager.event.PaymentMethodEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchPaymentMethodWorkflow(
    eventStream: EventStream
) : AbstractPaymentMethodWorkflow<SearchPaymentMethodRequest, SearchPaymentMethodResponse>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(
        request: SearchPaymentMethodRequest,
        response: SearchPaymentMethodResponse,
        context: WorkflowContext
    ): PaymentMethodEventPayload? = null

    override fun getValidationRules(request: SearchPaymentMethodRequest, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(request: SearchPaymentMethodRequest, context: WorkflowContext): SearchPaymentMethodResponse {
        val paymentMethods = checkoutAccessApi.searchPaymentMethod(
            request = com.wutsi.checkout.access.dto.SearchPaymentMethodRequest(
                accountId = getCurrentAccountId(context),
                status = request.status,
                limit = request.limit,
                offset = request.offset
            )
        ).paymentMethods
        return SearchPaymentMethodResponse(
            paymentMethods = paymentMethods.map {
                PaymentMethodSummary(
                    accountId = it.accountId,
                    token = it.token,
                    type = it.type,
                    number = it.number,
                    status = it.status,
                    updated = it.updated,
                    created = it.created,
                    deactivated = it.deactivated,
                    provider = PaymentProviderSummary(
                        id = it.provider.id,
                        name = it.provider.name,
                        logoUrl = it.provider.logoUrl,
                        code = it.provider.code,
                        type = it.provider.type
                    )
                )
            }
        )
    }
}
