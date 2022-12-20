package com.wutsi.checkout.manager.dto

import kotlin.collections.List

public data class SearchPaymentProviderResponse(
    public val paymentProviders: List<PaymentProviderSummary> = emptyList(),
)
