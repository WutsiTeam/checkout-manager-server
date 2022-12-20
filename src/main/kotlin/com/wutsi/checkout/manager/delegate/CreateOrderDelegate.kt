package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import com.wutsi.checkout.manager.workflow.CreateOrderWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CreateOrderDelegate(
    private val logger: KVLogger,
    private val workflow: CreateOrderWorkflow,
) {
    public fun invoke(request: CreateOrderRequest): CreateOrderResponse {
        logger.add("request_customer_email", request.customerEmail)
        logger.add("request_customer_name", request.customerName)
        logger.add("request_business_id", request.businessId)
        logger.add("request_channel_type", request.channelType)
        logger.add("request_device_type", request.deviceType)

        val response = workflow.execute(request, WorkflowContext())
        logger.add("response_order_id", response.orderId)
        return response
    }
}
