package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.GetOrderResponse
import com.wutsi.checkout.manager.dto.Order
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetOrderWorkflow : AbstractQueryWorkflow<String, GetOrderResponse>() {
    override fun execute(orderId: String, context: WorkflowContext): GetOrderResponse {
        val order = checkoutAccessApi.getOrder(orderId).order
        val json = objectMapper.writeValueAsString(order)
        return GetOrderResponse(
            order = objectMapper.readValue(json, Order::class.java),
        )
    }
}
