package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.WorkflowContext

abstract class AbstractOrderWorkflow<Req, Resp>(
    eventStream: EventStream,
) : AbstractCheckoutWorkflow<Req, Resp, OrderEventPayload>(eventStream) {
    protected fun getOrder(id: String, context: WorkflowContext): Order {
        val key = "order.$id"
        if (!context.data.containsKey(key) || (context.data[key] !is Order)) {
            val order = checkoutAccessApi.getOrder(id).order
            context.data[key] = order
            return order
        }
        return context.data[key] as Order
    }
}
