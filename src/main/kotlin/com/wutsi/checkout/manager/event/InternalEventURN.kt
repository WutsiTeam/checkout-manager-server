package com.wutsi.checkout.manager.event

enum class InternalEventURN(val urn: String) {
    TRANSACTION_PENDING("urn:wutsi:event:checkout-manager:transaction-pending"),
    ORDER_EXPIRED("urn:wutsi:event:checkout-manager:order-expired")
}
