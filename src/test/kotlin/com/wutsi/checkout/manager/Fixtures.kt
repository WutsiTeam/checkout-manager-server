package com.wutsi.checkout.manager

import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.checkout.access.enums.PaymentMethodType
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.access.dto.Phone
import com.wutsi.membership.access.enums.AccountStatus

object Fixtures {
    fun createAccount(
        id: Long = System.currentTimeMillis(),
        status: AccountStatus = AccountStatus.ACTIVE,
        business: Boolean = false,
        country: String = "CM",
        phoneNumber: String = "+237670000010",
        displayName: String = "Ray Sponsible"
    ) = Account(
        id = id,
        displayName = displayName,
        status = status.name,
        business = business,
        country = country,
        phone = Phone(
            number = phoneNumber,
            country = country
        )
    )

    fun createPaymentProvider(
        id: Long = System.currentTimeMillis(),
        type: PaymentMethodType = PaymentMethodType.MOBILE_MONEY,
        code: String = "MTN"
    ) = PaymentProviderSummary(
        id = id,
        code = code,
        type = type.name
    )
}
