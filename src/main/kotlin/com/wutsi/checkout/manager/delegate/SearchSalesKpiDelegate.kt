package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.SearchSalesKpiRequest
import com.wutsi.checkout.manager.dto.SearchSalesKpiResponse
import com.wutsi.checkout.manager.workflow.SearchSalesKpiWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchSalesKpiDelegate(
    private val logger: KVLogger,
    private val workflow: SearchSalesKpiWorkflow,
) {
    public fun invoke(request: SearchSalesKpiRequest): SearchSalesKpiResponse {
        logger.add("request_from_date", request.fromDate)
        logger.add("request_to_date", request.toDate)
        logger.add("request_business_id", request.businessId)
        logger.add("request_product_id", request.productId)
        logger.add("request_aggregate", request.aggregate)

        return workflow.execute(request, WorkflowContext())
    }
}
