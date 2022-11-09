package com.wutsi.checkout.manager

import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.checkout.access.enums.PaymentMethodType
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.access.dto.AccountSummary
import com.wutsi.membership.access.dto.Phone
import com.wutsi.membership.access.enums.AccountStatus

object Fixtures {
    fun createAccountSummary() = AccountSummary()

    fun createAccount(
        status: AccountStatus = AccountStatus.ACTIVE,
        business: Boolean = false,
        country: String = "CM",
        phoneNumber: String = "+237670000010",
        displayName: String = "Ray Sponsible"
    ) = Account(
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
        type: PaymentMethodType = PaymentMethodType.MOBILE_MONEY
    ) = PaymentProviderSummary(
        id = id,
        code = "MTN",
        type = type.name
    )
}
