package com.wutsi.checkout.manager.dto

import kotlin.String

public data class SearchPaymentProviderRequest(
    public val country: String? = null,
    public val number: String? = null,
    public val type: String? = null,
)
