package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.CreatePaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodResponse
import com.wutsi.event.EventURN
import com.wutsi.event.PaymentMethodEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import org.springframework.stereotype.Service

@Service
class AddPaymentMethodWorkflow(
    eventStream: EventStream,
) : AbstractPaymentMethodWorkflow<AddPaymentMethodRequest, AddPaymentMethodResponse>(eventStream) {
    override fun getEventType(
        request: AddPaymentMethodRequest,
        response: AddPaymentMethodResponse,
        context: WorkflowContext,
    ) = EventURN.PAYMENT_METHOD_ADDED.urn

    override fun toEventPayload(
        request: AddPaymentMethodRequest,
        response: AddPaymentMethodResponse,
        context: WorkflowContext,
    ) = PaymentMethodEventPayload(
        accountId = getCurrentAccountId(context),
        paymentMethodToken = response.paymentMethodToken,
    )

    override fun getValidationRules(request: AddPaymentMethodRequest, context: WorkflowContext): RuleSet {
        val account = getCurrentAccount(context)
        return RuleSet(
            listOf(
                AccountShouldBeActiveRule(account),
            ),
        )
    }

    override fun doExecute(request: AddPaymentMethodRequest, context: WorkflowContext): AddPaymentMethodResponse =
        AddPaymentMethodResponse(
            paymentMethodToken = checkoutAccessApi.createPaymentMethod(
                request = CreatePaymentMethodRequest(
                    accountId = getCurrentAccountId(context),
                    type = request.type,
                    number = request.number,
                    country = request.country,
                    ownerName = request.ownerName,
                    providerId = request.providerId,
                ),
            ).paymentMethodToken,
        )
}
