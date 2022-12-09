package com.wutsi.checkout.manager.dto

import javax.validation.constraints.NotBlank

public data class UpdateOrderStatusRequest(
    @get:NotBlank
    public val orderId: String = "",
    @get:NotBlank
    public val status: String = "",
    public val reason: String? = null
)
