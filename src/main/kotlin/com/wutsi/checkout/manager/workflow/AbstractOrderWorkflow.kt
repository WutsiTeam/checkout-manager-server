package com.wutsi.checkout.manager.workflow

import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream

abstract class AbstractOrderWorkflow<Req, Resp>(
    eventStream: EventStream
) : AbstractCheckoutWorkflow<Req, Resp, OrderEventPayload>(eventStream)
