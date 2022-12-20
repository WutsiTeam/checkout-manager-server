package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.SearchPaymentMethodRequest
import com.wutsi.checkout.manager.dto.SearchPaymentMethodResponse
import com.wutsi.checkout.manager.workflow.SearchPaymentMethodWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchPaymentMethodDelegate(
    private val logger: KVLogger,
    private val workflow: SearchPaymentMethodWorkflow,
) {
    public fun invoke(request: SearchPaymentMethodRequest): SearchPaymentMethodResponse {
        logger.add("request_status", request.status)
        logger.add("request_limit", request.limit)
        logger.add("request_offset", request.offset)

        val response = workflow.execute(request, WorkflowContext())
        logger.add("response_count", response.paymentMethods.size)

        return response
    }
}
