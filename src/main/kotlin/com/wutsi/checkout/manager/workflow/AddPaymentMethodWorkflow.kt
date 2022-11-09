package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.CreatePaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodResponse
import com.wutsi.checkout.manager.event.EventURN
import com.wutsi.checkout.manager.event.PaymentMethodEventPayload
import com.wutsi.checkout.manager.util.SecurityUtil
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.CountryRegulations
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.CountrySupportsBusinessAccountRule
import org.springframework.stereotype.Service

@Service
class AddPaymentMethodWorkflow(
    private val countryRegulations: CountryRegulations,
    eventStream: EventStream
) : AbstractPaymentMethodWorkflow(eventStream) {
    override fun getEventType() = EventURN.PAYMENT_METHOD_ADDED.urn

    override fun toMemberEventPayload(context: WorkflowContext) = PaymentMethodEventPayload(
        accountId = SecurityUtil.getAccountId(),
        paymentMethodToken = (context.response as AddPaymentMethodResponse).paymentMethodToken
    )

    override fun getValidationRules(context: WorkflowContext): RuleSet {
        val account = getCurrentAccount(context)
        return RuleSet(
            listOf(
                AccountShouldBeActiveRule(account),
                CountrySupportsBusinessAccountRule(account, countryRegulations)
            )
        )
    }

    override fun doExecute(context: WorkflowContext) {
        val request = context.request as AddPaymentMethodRequest
        val token = checkoutAccess.createPaymentMethod(
            request = CreatePaymentMethodRequest(
                accountId = context.accountId ?: SecurityUtil.getAccountId(),
                type = request.type,
                number = request.number,
                country = request.country,
                ownerName = request.ownerName,
                providerId = request.providerId
            )
        ).paymentMethodToken

        context.response = AddPaymentMethodResponse(
            paymentMethodToken = token
        )
    }
}
