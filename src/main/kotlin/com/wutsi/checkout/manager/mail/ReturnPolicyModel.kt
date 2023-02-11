package com.wutsi.checkout.manager.mail

public data class ReturnPolicyModel(
    public val accepted: Boolean = false,
    public val contactWindow: Int = 0,
    public val shipBackWindow: Int = 0,
    public val message: String? = null,
)
