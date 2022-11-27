package com.wutsi.checkout.manager.event

enum class InternalEventURN(val urn: String) {
    CHARGE_SUCESSFULL("urn:wutsi:event:checkout-manager:charge-successfull"),
    CASHOUT_SUCESSFULL("urn:wutsi:event:checkout-manager:cashout-successfull"),
    TRANSACTION_PENDING("urn:wutsi:event:checkout-manager:transaction-pending")
}
