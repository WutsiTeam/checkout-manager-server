package com.wutsi.checkout.manager.dto

import kotlin.collections.List

public data class SearchSalesKpiResponse(
    public val kpis: List<SalesKpiSummary> = emptyList(),
)
