package com.wutsi.checkout.manager.workflow

import com.wutsi.event.BusinessEventPayload
import com.wutsi.platform.core.stream.EventStream

abstract class AbstractBusinessWorkflow<Req, Resp>(eventStream: EventStream) :
    AbstractCheckoutWorkflow<Req, Resp, BusinessEventPayload>(eventStream)
