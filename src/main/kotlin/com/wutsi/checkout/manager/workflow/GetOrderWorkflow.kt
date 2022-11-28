package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.dto.GetOrderResponse
import com.wutsi.checkout.manager.dto.Order
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetOrderWorkflow(
    private val objectMapper: ObjectMapper,
    eventStream: EventStream
) : AbstractOrderWorkflow<String, GetOrderResponse>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(
        orderId: String,
        response: GetOrderResponse,
        context: WorkflowContext
    ): OrderEventPayload? = null

    override fun getValidationRules(orderId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(orderId: String, context: WorkflowContext): GetOrderResponse {
        val order = checkoutAccessApi.getOrder(orderId).order

        /*
         * Because the order data structure is the same as the one from access layer, let use json serialization for copying the data
         */
        val json = objectMapper.writeValueAsString(order)
        return GetOrderResponse(
            order = objectMapper.readValue(json, Order::class.java)
        )
    }
}
