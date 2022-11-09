package com.wutsi.checkout.manager.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size
import kotlin.Long
import kotlin.String

public data class AddPaymentMethodRequest(
    public val providerId: Long = 0,
    @get:NotBlank
    public val type: String = "",
    @get:NotBlank
    public val number: String = "",
    @get:NotBlank
    @get:Size(
        min = 2,
        max = 2
    )
    public val country: String = "",
    @get:NotBlank
    @get:Size(max = 100)
    public val ownerName: String = ""
)
