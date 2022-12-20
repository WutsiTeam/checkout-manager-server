package com.wutsi.checkout.manager.dto

import kotlin.Long
import kotlin.String

public data class PaymentProviderSummary(
    public val id: Long = 0,
    public val code: String = "",
    public val name: String = "",
    public val type: String = "",
    public val logoUrl: String = "",
)
