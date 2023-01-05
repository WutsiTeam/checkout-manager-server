package com.wutsi.checkout.manager.dto

import kotlin.Int
import kotlin.Long
import kotlin.collections.List

public data class CreateOrderItemRequest(
    public val productId: Long = 0,
    public val quantity: Int = 0,
    public val discounts: List<CreateOrderDiscountRequest> = emptyList(),
)
