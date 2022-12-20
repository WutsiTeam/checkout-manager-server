package com.wutsi.checkout.manager.event

enum class InternalEventURN(val urn: String) {
    TRANSACTION_SUCCESSFUL("urn:wutsi:event:checkout-manager:transaction-successful"),
    ORDER_TO_CUSTOMER_SUBMITTED("urn:wutsi:event:checkout-manager:order-to-customer-submitted"),
    ORDER_TO_MERCHANT_SUBMITTED("urn:wutsi:event:checkout-manager:order-to-merchant-submitted"),
    ORDER_FULLFILLED("urn:wutsi:event:checkout-manager:order-fulfilled"),
}
