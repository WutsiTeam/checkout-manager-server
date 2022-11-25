package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.GetPaymentMethodResponse
import com.wutsi.checkout.manager.dto.PaymentMethod
import com.wutsi.checkout.manager.dto.PaymentProviderSummary
import com.wutsi.checkout.manager.event.PaymentMethodEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetPaymentMethodWorkflow(
    eventStream: EventStream
) : AbstractPaymentMethodWorkflow<String, GetPaymentMethodResponse>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(
        token: String,
        response: GetPaymentMethodResponse,
        context: WorkflowContext
    ): PaymentMethodEventPayload? = null

    override fun getValidationRules(token: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(token: String, context: WorkflowContext): GetPaymentMethodResponse {
        val paymentMethod = checkoutAccessApi.getPaymentMethod(token).paymentMethod
        return GetPaymentMethodResponse(
            paymentMethod = PaymentMethod(
                accountId = paymentMethod.accountId,
                country = paymentMethod.country,
                updated = paymentMethod.updated,
                created = paymentMethod.created,
                status = paymentMethod.status,
                type = paymentMethod.type,
                number = paymentMethod.number,
                ownerName = paymentMethod.ownerName,
                token = token,
                deactivated = paymentMethod.deactivated,
                provider = PaymentProviderSummary(
                    id = paymentMethod.provider.id,
                    name = paymentMethod.provider.name,
                    logoUrl = paymentMethod.provider.logoUrl,
                    code = paymentMethod.provider.code,
                    type = paymentMethod.provider.type
                )
            )
        )
    }
}
