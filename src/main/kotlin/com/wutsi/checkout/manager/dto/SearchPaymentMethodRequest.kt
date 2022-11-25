package com.wutsi.checkout.manager.dto

public data class SearchPaymentMethodRequest(
    public val status: String? = null,
    public val limit: Int = 100,
    public val offset: Int = 0
)
