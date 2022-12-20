package com.wutsi.checkout.manager.dto

import kotlin.collections.List

public data class SearchTransactionResponse(
    public val transactions: List<TransactionSummary> = emptyList(),
)
