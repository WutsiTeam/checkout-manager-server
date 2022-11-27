package com.wutsi.checkout.manager.event

enum class InternalEventURN(val urn: String) {
    CHARGE_SUCESSFULL("urn:wutsi:event:checkout-manager:charge-successfull"),
    CHARGE_FAILED("urn:wutsi:event:checkout-manager:charge-failed")
}
