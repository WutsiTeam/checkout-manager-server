package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.manager.dto.CreateBusinessRequest
import com.wutsi.checkout.manager.workflow.task.UpdateAccountAttributeTask
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.access.dto.EnableBusinessRequest
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.CountryShouldSupportBusinessAccountRule
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class CreateBusinessWorkflow(
    private val regulationEngine: RegulationEngine,
    private val membershipAccessApi: MembershipAccessApi,
    private val checkoutAccessApi: CheckoutAccessApi,
    private val workflowEngine: WorkflowEngine,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "create-business")
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        val account = getCurrentAccount(context)
        validate(account)

        val businessId = createBusiness(account)
        enableBusinessAccount(account, context.input as CreateBusinessRequest)
        setAccountBusinessId(account, businessId, context)
    }

    private fun getCurrentAccount(context: WorkflowContext): Account =
        membershipAccessApi.getAccount(context.accountId!!).account

    private fun validate(account: Account) {
        RuleSet(
            listOf(
                AccountShouldBeActiveRule(account),
                CountryShouldSupportBusinessAccountRule(account, regulationEngine),
            ),
        ).check()
    }

    private fun enableBusinessAccount(account: Account, request: CreateBusinessRequest) =
        membershipAccessApi.enableBusiness(
            id = account.id,
            request = EnableBusinessRequest(
                displayName = request.displayName,
                country = account.country,
                cityId = request.cityId,
                categoryId = request.categoryId,
                biography = request.biography,
                whatsapp = request.whatsapp,
                email = request.email,
            ),
        )

    private fun createBusiness(account: Account): Long =
        checkoutAccessApi.createBusiness(
            request = com.wutsi.checkout.access.dto.CreateBusinessRequest(
                accountId = account.id,
                country = account.country,
                currency = regulationEngine.country(account.country).currency,
            ),
        ).businessId

    private fun setAccountBusinessId(account: Account, businessId: Long, context: WorkflowContext) =
        workflowEngine.executeAsync(
            id = UpdateAccountAttributeTask.ID,
            context = WorkflowContext(
                accountId = account.id,
                data = mutableMapOf(
                    UpdateAccountAttributeTask.CONTEXT_ATTR_NAME to "business-id",
                    UpdateAccountAttributeTask.CONTEXT_ATTR_VALUE to businessId,
                ),
            ),
        )
}
