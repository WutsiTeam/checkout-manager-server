package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.dto.OrderSummary
import com.wutsi.checkout.manager.dto.SearchOrderRequest
import com.wutsi.checkout.manager.dto.SearchOrderResponse
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchOrderWorkflow(
    private val objectMapper: ObjectMapper,
    eventStream: EventStream,
) : AbstractOrderWorkflow<SearchOrderRequest, SearchOrderResponse>(eventStream) {
    override fun getEventType(
        request: SearchOrderRequest,
        response: SearchOrderResponse,
        context: WorkflowContext,
    ): String? = null

    override fun toEventPayload(
        request: SearchOrderRequest,
        response: SearchOrderResponse,
        context: WorkflowContext,
    ): OrderEventPayload? = null

    override fun getValidationRules(request: SearchOrderRequest, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(request: SearchOrderRequest, context: WorkflowContext): SearchOrderResponse {
        val orders = checkoutAccessApi.searchOrder(
            request = com.wutsi.checkout.access.dto.SearchOrderRequest(
                customerId = request.customerId,
                limit = request.limit,
                offset = request.offset,
                businessId = request.businessId,
                status = request.status,
                createdTo = request.createdTo,
                createdFrom = request.createdFrom,
                expiresTo = request.expiresTo,
            ),
        ).orders
        return SearchOrderResponse(
            orders = orders.map {
                objectMapper.readValue(
                    objectMapper.writeValueAsString(it),
                    OrderSummary::class.java,
                )
            },
        )
    }
}
