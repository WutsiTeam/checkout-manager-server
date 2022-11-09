package com.wutsi.checkout.manager.event

data class PaymentMethodEventPayload(
    val accountId: Long = -1,
    val paymentMethodToken: String = ""
)
