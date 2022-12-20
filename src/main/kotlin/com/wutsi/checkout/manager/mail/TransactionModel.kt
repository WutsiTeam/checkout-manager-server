package com.wutsi.checkout.manager.mail

data class TransactionModel(
    val id: String,
    val type: String,
    val amount: String,
    val paymentMethod: PaymentMethodModel,
)
