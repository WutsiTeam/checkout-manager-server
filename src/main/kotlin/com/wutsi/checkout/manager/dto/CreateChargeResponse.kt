package com.wutsi.checkout.manager.dto

import kotlin.String

public data class CreateChargeResponse(
    public val transactionId: String = "",
    public val status: String = "",
)
