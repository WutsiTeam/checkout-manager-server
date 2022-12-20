package com.wutsi.checkout.manager.mail

data class PaymentMethodModel(
    val type: String,
    val number: String,
    val maskedNumber: String,
    val provider: PaymentProviderModel,
)
