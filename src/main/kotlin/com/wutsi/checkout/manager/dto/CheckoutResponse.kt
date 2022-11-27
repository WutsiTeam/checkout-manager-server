package com.wutsi.checkout.manager.dto

public data class CheckoutResponse(
    public val orderId: String = "",
    public val transactionId: String = "",
    public val transactionStatus: String = ""
)
