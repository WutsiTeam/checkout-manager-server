package com.wutsi.checkout.manager.mail

data class PaymentProviderModel(
    val id: Long,
    val code: String,
    val name: String,
    val logoUrl: String,
)
