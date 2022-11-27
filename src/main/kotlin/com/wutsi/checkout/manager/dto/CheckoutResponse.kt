package com.wutsi.checkout.manager.dto

import kotlin.String

public data class CheckoutResponse(
    public val orderId: String = "",
    public val transactionId: String = "",
    public val transactionStatus: String = ""
)
