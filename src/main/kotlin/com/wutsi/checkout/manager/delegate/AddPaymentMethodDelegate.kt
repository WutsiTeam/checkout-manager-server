package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodResponse
import com.wutsi.checkout.manager.workflow.AddPaymentMethodWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class AddPaymentMethodDelegate(
    private val workflow: AddPaymentMethodWorkflow,
    private val logger: KVLogger,
) {
    fun invoke(request: AddPaymentMethodRequest): AddPaymentMethodResponse {
        logger.add("request_provider_id", request.providerId)
        logger.add("request_country", request.country)
        logger.add("request_number", "...." + request.number.takeLast(4))
        logger.add("request_type", request.type)
        logger.add("request_owner_name", request.ownerName)

        val response = workflow.execute(request, WorkflowContext())
        logger.add("response_payment_method_token", response.paymentMethodToken)
        return response
    }
}
