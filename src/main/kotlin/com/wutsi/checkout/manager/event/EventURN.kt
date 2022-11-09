package com.wutsi.checkout.manager.event

enum class EventURN(val urn: String) {
    PAYMENT_METHOD_ADDED("urn:wutsi:event:checkout-manager:payment-method-added"),
    BUSINESS_CREATED("urn:wutsi:event:checkout-manager:business-created"),
    BUSINESS_SUSPENDED("urn:wutsi:event:checkout-manager:business-suspended")
}
