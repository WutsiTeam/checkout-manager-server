package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.OrderSummary
import com.wutsi.checkout.manager.dto.SearchOrderRequest
import com.wutsi.checkout.manager.dto.SearchOrderResponse
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchOrderWorkflow : AbstractQueryWorkflow<SearchOrderRequest, SearchOrderResponse>() {
    override fun execute(request: SearchOrderRequest, context: WorkflowContext): SearchOrderResponse {
        val orders = checkoutAccessApi.searchOrder(
            request = com.wutsi.checkout.access.dto.SearchOrderRequest(
                customerAccountId = request.customerAccountId,
                limit = request.limit,
                offset = request.offset,
                businessId = request.businessId,
                status = request.status,
                createdTo = request.createdTo,
                createdFrom = request.createdFrom,
                expiresTo = request.expiresTo,
                productId = request.productId,
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
