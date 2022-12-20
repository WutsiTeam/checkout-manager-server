package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.manager.workflow.SendOrderToCustomerWorkflow
import com.wutsi.checkout.manager.workflow.SendOrderToMerchantWorkflow
import com.wutsi.checkout.manager.workflow.UpdateOrderStatusWorkflow
import com.wutsi.enums.OrderStatus
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class OrderEventHandler(
    private val mapper: ObjectMapper,
    private val logger: KVLogger,
    private val checkoutAccessApi: CheckoutAccessApi,
    private val sendOrderToCustomerWorkflow: SendOrderToCustomerWorkflow,
    private val sendOrderToMerchantWorkflow: SendOrderToMerchantWorkflow,
    private val updateOrderStatusWorkflow: UpdateOrderStatusWorkflow,
) {
    fun onSendToCustomer(event: Event) {
        val payload = toOrderEventPayload(event)
        log(payload)

        sendOrderToCustomerWorkflow.execute(payload.orderId, WorkflowContext())
    }

    fun onSendToMerchant(event: Event) {
        val payload = toOrderEventPayload(event)
        log(payload)

        sendOrderToMerchantWorkflow.execute(payload.orderId, WorkflowContext())
    }

    fun onOrderFulfilled(event: Event) {
        val payload = toOrderEventPayload(event)
        log(payload)

        val order = checkoutAccessApi.getOrder(payload.orderId).order
        val context = WorkflowContext(accountId = order.business.accountId)
        val request = UpdateOrderStatusRequest(
            orderId = payload.orderId,
            status = OrderStatus.COMPLETED.name,
        )
        updateOrderStatusWorkflow.execute(request, context)
    }

    private fun toOrderEventPayload(event: Event): OrderEventPayload =
        mapper.readValue(event.payload, OrderEventPayload::class.java)

    private fun log(payload: OrderEventPayload) {
        logger.add("payload_order_id", payload.orderId)
    }
}
