package com.wutsi.checkout.manager.event

data class BusinessEventPayload(
    val accountId: Long = -1,
    val businessId: Long = -1
)
