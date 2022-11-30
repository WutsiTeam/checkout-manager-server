package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class ExpireOrderWorkflow(
    eventStream: EventStream
) : AbstractOrderWorkflow<String, Unit>(eventStream) {
    override fun getEventType() = EventURN.ORDER_EXPIRED.urn

    override fun toEventPayload(
        orderId: String,
        response: Unit,
        context: WorkflowContext
    ) = OrderEventPayload(orderId)

    override fun getValidationRules(orderId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(orderId: String, context: WorkflowContext) {
        checkoutAccessApi.updateOrderStatus(orderId, UpdateOrderStatusRequest(OrderStatus.EXPIRED.name))
    }
}
