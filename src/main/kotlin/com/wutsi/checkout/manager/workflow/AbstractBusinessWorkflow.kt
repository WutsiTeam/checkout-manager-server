package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.event.BusinessEventPayload
import com.wutsi.platform.core.stream.EventStream

abstract class AbstractBusinessWorkflow(eventStream: EventStream) :
    AbstractCheckoutWorkflow<BusinessEventPayload>(eventStream)
