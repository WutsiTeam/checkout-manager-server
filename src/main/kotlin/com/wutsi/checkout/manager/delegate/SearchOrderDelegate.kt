package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.SearchOrderRequest
import com.wutsi.checkout.manager.dto.SearchOrderResponse
import com.wutsi.checkout.manager.workflow.SearchOrderWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchOrderDelegate(
    private val logger: KVLogger,
    private val workflow: SearchOrderWorkflow,
) {
    public fun invoke(request: SearchOrderRequest): SearchOrderResponse {
        logger.add("request_limit", request.limit)
        logger.add("request_offset", request.offset)
        logger.add("request_status", request.status)
        logger.add("request_created_from", request.createdFrom)
        logger.add("request_created_to", request.createdTo)
        logger.add("request_expires_to", request.expiresTo)
        logger.add("request_business_id", request.businessId)
        logger.add("request_customer_account_id", request.customerAccountId)

        val response = workflow.execute(request, WorkflowContext())
        logger.add("response_count", response.orders.size)

        return response
    }
}
