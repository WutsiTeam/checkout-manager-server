package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.workflow.ExpireOrderWorkflow
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class OrderEventHandler(
    private val mapper: ObjectMapper,
    private val logger: KVLogger,
    private val expireOrderWorkflow: ExpireOrderWorkflow
) {
    fun onOrderExpired(event: Event) {
        val payload = toOrderEventPayload(event)
        log(payload)

        expireOrderWorkflow.execute(payload.orderId, WorkflowContext())
    }

    private fun toOrderEventPayload(event: Event): OrderEventPayload =
        mapper.readValue(event.payload, OrderEventPayload::class.java)

    private fun log(payload: OrderEventPayload) {
        logger.add("payload_order_id", payload.orderId)
    }
}
