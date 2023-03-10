package com.wutsi.checkout.manager.workflow.task

import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.UpdateAccountAttributeRequest
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class UpdateAccountAttributeTask(
    private val workflowEngine: WorkflowEngine,
    private val membershipAccessApi: MembershipAccessApi,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "update-account-attribute")
        const val CONTEXT_ATTR_NAME = "name"
        const val CONTEXT_ATTR_VALUE = "value"
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        membershipAccessApi.updateAccountAttribute(
            id = context.accountId!!,
            request = UpdateAccountAttributeRequest(
                name = context.data[CONTEXT_ATTR_NAME]!!.toString(),
                value = context.data[CONTEXT_ATTR_VALUE]?.toString(),
            ),
        )
    }
}
