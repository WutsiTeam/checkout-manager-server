package com.wutsi.checkout.manager.dto

import kotlin.collections.List

public data class SearchOrderResponse(
    public val orders: List<OrderSummary> = emptyList(),
)
