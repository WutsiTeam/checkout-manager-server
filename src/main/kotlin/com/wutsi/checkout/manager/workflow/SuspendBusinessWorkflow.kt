package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.UpdateBusinessStatusRequest
import com.wutsi.checkout.access.enums.BusinessStatus
import com.wutsi.checkout.manager.event.BusinessEventPayload
import com.wutsi.checkout.manager.event.EventURN
import com.wutsi.membership.access.dto.UpdateAccountAttributeRequest
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SuspendBusinessWorkflow(
    eventStream: EventStream
) : AbstractBusinessWorkflow(eventStream) {
    override fun getEventType() = EventURN.BUSINESS_SUSPENDED.urn

    override fun toEventPayload(context: WorkflowContext) = context.response?.let {
        BusinessEventPayload(
            accountId = getCurrentAccountId(context),
            businessId = it as Long
        )
    }

    override fun getValidationRules(context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(context: WorkflowContext) {
        val account = getCurrentAccount(context)
        if (account.businessId != null) {
            checkoutAccess.updateBusinessStatus(
                account.businessId!!,
                UpdateBusinessStatusRequest(
                    status = BusinessStatus.SUSPENDED.name
                )
            )

            membershipAccess.updateAccountAttribute(
                id = account.id,
                request = UpdateAccountAttributeRequest(
                    name = "business-id",
                    value = null
                )
            )

            context.response = account.businessId
        }
    }
}
