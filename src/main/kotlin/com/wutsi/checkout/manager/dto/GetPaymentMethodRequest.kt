package com.wutsi.checkout.manager.dto

public data class GetPaymentMethodRequest(
    public val paymentMethod: PaymentMethod = PaymentMethod(),
)
