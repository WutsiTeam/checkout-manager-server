package com.wutsi.checkout.manager.mail

public data class CancellationPolicyModel(
    public val accepted: Boolean = false,
    public val window: Int = 0,
    public val message: String? = null,
)
