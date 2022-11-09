package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.CreateBusinessRequest
import com.wutsi.checkout.manager.event.BusinessEventPayload
import com.wutsi.checkout.manager.event.EventURN
import com.wutsi.membership.access.dto.UpdateAccountAttributeRequest
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.CountryRegulations
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.CountrySupportsBusinessAccountRule
import org.springframework.stereotype.Service

@Service
class CreateBusinessWorkflow(
    private val countryRegulations: CountryRegulations,
    eventStream: EventStream
) : AbstractBusinessWorkflow(eventStream) {
    override fun getEventType() = EventURN.BUSINESS_CREATED.urn

    override fun toEventPayload(context: WorkflowContext) = BusinessEventPayload(
        accountId = getCurrentAccountId(context),
        businessId = context.response as Long
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
        val account = getCurrentAccount(context)
        val businessId = checkoutAccess.createBusiness(
            request = CreateBusinessRequest(
                accountId = account.id,
                country = account.country,
                currency = countryRegulations.get(account.country).currency
            )
        ).businessId

        membershipAccess.updateAccountAttribute(
            id = account.id,
            request = UpdateAccountAttributeRequest(
                name = "business-id",
                value = businessId.toString()
            )
        )

        context.response = businessId
    }
}
