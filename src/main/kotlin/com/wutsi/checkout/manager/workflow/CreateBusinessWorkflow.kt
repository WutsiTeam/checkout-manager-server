package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.CreateBusinessRequest
import com.wutsi.event.BusinessEventPayload
import com.wutsi.event.EventURN
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.CountryShouldSupportBusinessAccountRule
import org.springframework.stereotype.Service

@Service
class CreateBusinessWorkflow(
    private val regulationEngine: RegulationEngine,
    eventStream: EventStream
) : AbstractBusinessWorkflow<Void?, Long>(eventStream) {
    override fun getEventType(request: Void?, businessId: Long, context: WorkflowContext) =
        EventURN.BUSINESS_CREATED.urn

    override fun toEventPayload(request: Void?, businessId: Long, context: WorkflowContext) = BusinessEventPayload(
        accountId = getCurrentAccountId(context),
        businessId = businessId
    )

    override fun getValidationRules(request: Void?, context: WorkflowContext): RuleSet {
        val account = getCurrentAccount(context)
        return RuleSet(
            listOf(
                AccountShouldBeActiveRule(account),
                CountryShouldSupportBusinessAccountRule(account, regulationEngine)
            )
        )
    }

    override fun doExecute(request: Void?, context: WorkflowContext): Long {
        val account = getCurrentAccount(context)
        return checkoutAccessApi.createBusiness(
            request = CreateBusinessRequest(
                accountId = account.id,
                country = account.country,
                currency = regulationEngine.country(account.country).currency
            )
        ).businessId
    }
}
