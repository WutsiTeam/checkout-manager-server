package com.wutsi.checkout.manager.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class CreateBusinessRequest(
    @get:NotBlank
    @get:Size(max = 50)
    public val displayName: String = "",
    @get:NotNull
    public val categoryId: Long = 0,
    @get:NotNull
    public val cityId: Long = 0,
    @get:Size(max = 160)
    public val biography: String? = null,
    public val whatsapp: Boolean = false,
    @get:NotBlank
    public val email: String = "",
)
