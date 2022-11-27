package com.wutsi.checkout.manager.dto

import kotlin.Int
import kotlin.Long

public data class CreateOrderItemRequest(
    public val productId: Long = 0,
    public val quantity: Int = 0
)
