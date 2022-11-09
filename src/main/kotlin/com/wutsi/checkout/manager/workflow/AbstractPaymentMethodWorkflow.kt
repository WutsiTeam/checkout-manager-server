package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.event.PaymentMethodEventPayload
import com.wutsi.platform.core.stream.EventStream

abstract class AbstractPaymentMethodWorkflow(eventStream: EventStream) :
    AbstractCheckoutWorkflow<PaymentMethodEventPayload>(eventStream)
