package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.CreateChargeRequest
import com.wutsi.checkout.manager.dto.CreateChargeResponse
import com.wutsi.checkout.manager.workflow.CreateChargeWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CreateChargeDelegate(
    private val logger: KVLogger,
    private val workflow: CreateChargeWorkflow,
) {
    public fun invoke(request: CreateChargeRequest): CreateChargeResponse {
        logger.add("request_order_id", request.orderId)
        logger.add("request_business_id", request.businessId)
        logger.add("request_payment_token", request.paymentMethodToken)
        logger.add("request_description", request.description)
        logger.add("request_idempotency_key", request.idempotencyKey)
        logger.add("request_customer_email", request.email)

        val response = workflow.execute(request, WorkflowContext())
        logger.add("response_transaction_id", response.transactionId)
        logger.add("response_stats", response.status)

        return response
    }
}
