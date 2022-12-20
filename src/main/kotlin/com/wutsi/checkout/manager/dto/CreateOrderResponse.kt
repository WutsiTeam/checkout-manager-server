package com.wutsi.checkout.manager.dto

import kotlin.String

public data class CreateOrderResponse(
    public val orderId: String = "",
    public val orderStatus: String = "",
)
