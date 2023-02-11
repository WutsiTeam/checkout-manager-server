package com.wutsi.checkout.manager.mail

public data class StoreModel(
    public val id: Long = 0,
    public val cancellationPolicy: CancellationPolicyModel = CancellationPolicyModel(),
    public val returnPolicy: ReturnPolicyModel = ReturnPolicyModel(),
)
