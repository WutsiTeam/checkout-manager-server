package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.UpdateBusinessStatusRequest
import com.wutsi.enums.BusinessStatus
import com.wutsi.event.BusinessEventPayload
import com.wutsi.event.EventURN
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class DeactivateBusinessWorkflow(
    eventStream: EventStream,
) : AbstractBusinessWorkflow<Long, Unit>(eventStream) {
    override fun getEventType(businessId: Long, response: Unit, context: WorkflowContext) =
        EventURN.BUSINESS_DEACTIVATED.urn

    override fun toEventPayload(businessId: Long, response: Unit, context: WorkflowContext) = businessId?.let {
        BusinessEventPayload(
            accountId = getCurrentAccountId(context),
            businessId = it,
        )
    }

    override fun getValidationRules(businessId: Long, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(businessId: Long, context: WorkflowContext) {
        checkoutAccessApi.updateBusinessStatus(
            id = businessId,
            request = UpdateBusinessStatusRequest(
                status = BusinessStatus.INACTIVE.name,
            ),
        )
    }
}
