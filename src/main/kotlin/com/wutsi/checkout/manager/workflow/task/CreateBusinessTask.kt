package com.wutsi.checkout.manager.workflow.task

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class CreateBusinessTask(
    private val workflowEngine: WorkflowEngine,
    private val checkoutAccessApi: CheckoutAccessApi,
    private val membershipAccessApi: MembershipAccessApi,
    private val regulationEngine: RegulationEngine,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "create-business")
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    private fun getCurrentAccount(context: WorkflowContext): Account =
        membershipAccessApi.getAccount(context.accountId!!).account

    override fun execute(context: WorkflowContext) {
        val account = getCurrentAccount(context)
        val businessId = createBusiness(account)
        setAccountBusinessId(account, businessId, context)
    }

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
