package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.workflow.CreateBusinessWorkflow
import com.wutsi.checkout.manager.workflow.DeactivateBusinessWorkflow
import com.wutsi.event.BusinessEventPayload
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class BusinessEventHandler(
    private val mapper: ObjectMapper,
    private val logger: KVLogger,
    private val createBusinessWorkflow: CreateBusinessWorkflow,
    private val deactivateBusinessWorkflow: DeactivateBusinessWorkflow,
) {
    fun onBusinessActivated(event: Event) {
        val payload = toBusinessPayload(event)
        log(payload)

        val context = WorkflowContext(accountId = payload.accountId)
        val businessId = createBusinessWorkflow.execute(null, context)
        logger.add("business_id", businessId)
    }

    fun onBusinessDeactivated(event: Event) {
        val payload = toBusinessPayload(event)
        log(payload)

        val context = WorkflowContext(accountId = payload.accountId)
        val businessId = deactivateBusinessWorkflow.execute(payload.businessId, context)
        logger.add("business_id", businessId)
    }

    private fun toBusinessPayload(event: Event): BusinessEventPayload =
        mapper.readValue(event.payload, BusinessEventPayload::class.java)

    private fun log(payload: BusinessEventPayload) {
        logger.add("payload_account_id", payload.accountId)
        logger.add("payload_business_id", payload.businessId)
    }
}
