package com.wutsi.checkout.manager.event

data class TransactionEventPayload(
    val transactionId: String = "",
    val orderId: String? = null
)
