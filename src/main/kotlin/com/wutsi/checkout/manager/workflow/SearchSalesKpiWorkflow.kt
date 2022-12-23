package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.SalesKpiSummary
import com.wutsi.checkout.manager.dto.SearchSalesKpiRequest
import com.wutsi.checkout.manager.dto.SearchSalesKpiResponse
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchSalesKpiWorkflow : AbstractQueryWorkflow<SearchSalesKpiRequest, SearchSalesKpiResponse>() {
    override fun execute(request: SearchSalesKpiRequest, context: WorkflowContext): SearchSalesKpiResponse {
        val kpis = checkoutAccessApi.searchSalesKpi(
            request = com.wutsi.checkout.access.dto.SearchSalesKpiRequest(
                businessId = request.businessId,
                productId = request.productId,
                fromDate = request.fromDate,
                toDate = request.toDate,
                aggregate = request.aggregate,
            ),
        ).kpis
        return SearchSalesKpiResponse(
            kpis = kpis.map {
                objectMapper.readValue(
                    objectMapper.writeValueAsString(it),
                    SalesKpiSummary::class.java,
                )
            },
        )
    }
}
