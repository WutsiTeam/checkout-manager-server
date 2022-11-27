package com.wutsi.checkout.manager.dto

import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class CheckoutRequest(
    public val deviceType: String? = null,
    public val channelType: String? = null,
    public val businessId: Long = 0,
    public val customerId: Long? = null,
    @get:NotBlank
    @get:Size(max = 100)
    public val customerName: String = "",
    @get:NotBlank
    @get:Size(max = 100)
    public val customerEmail: String = "",
    public val paymentMethodToken: String? = null,
    public val paymentMethodType: String? = null,
    public val paymentProviderId: Long? = null,
    @get:Size(max = 30)
    public val paymenMethodNumber: String? = null,
    public val productId: Long = 0,
    @get:Min(1)
    public val quantity: Int = 0,
    public val notes: String? = null,
    @get:NotBlank
    @get:Size(max = 36)
    public val idempotencyKey: String = ""
)
