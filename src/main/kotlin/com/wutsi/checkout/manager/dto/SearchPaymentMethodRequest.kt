package com.wutsi.checkout.manager.dto

import kotlin.Int
import kotlin.String

public data class SearchPaymentMethodRequest(
    public val status: String? = "ACTIVE",
    public val limit: Int = 100,
    public val offset: Int = 0,
)
