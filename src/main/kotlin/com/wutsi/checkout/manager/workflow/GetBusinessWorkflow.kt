package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.Business
import com.wutsi.checkout.manager.dto.GetBusinessResponse
import com.wutsi.event.BusinessEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetBusinessWorkflow(
    eventStream: EventStream
) : AbstractBusinessWorkflow<Long, GetBusinessResponse>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(
        businessId: Long,
        response: GetBusinessResponse,
        context: WorkflowContext
    ): BusinessEventPayload? = null

    override fun getValidationRules(businessId: Long, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(businessId: Long, context: WorkflowContext): GetBusinessResponse {
        val business = checkoutAccessApi.getBusiness(businessId).business
        return GetBusinessResponse(
            business = Business(
                id = business.id,
                accountId = business.accountId,
                balance = business.balance,
                currency = business.currency,
                status = business.status,
                country = business.country,
                created = business.created,
                updated = business.updated,
                deactivated = business.deactivated
            )
        )
    }
}
