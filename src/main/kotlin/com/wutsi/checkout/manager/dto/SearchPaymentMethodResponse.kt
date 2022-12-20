package com.wutsi.checkout.manager.dto

import kotlin.collections.List

public data class SearchPaymentMethodResponse(
    public val paymentMethods: List<PaymentMethodSummary> = emptyList(),
)
