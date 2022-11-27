package com.wutsi.checkout.manager

import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.PaymentMethod
import com.wutsi.checkout.access.dto.PaymentMethodSummary
import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.enums.AccountStatus
import com.wutsi.enums.BusinessStatus
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.access.dto.Phone

object Fixtures {
    fun createAccount(
        id: Long = System.currentTimeMillis(),
        status: AccountStatus = AccountStatus.ACTIVE,
        business: Boolean = false,
        businessId: Long? = null,
        country: String = "CM",
        phoneNumber: String = "+237670000010",
        displayName: String = "Ray Sponsible"
    ) = Account(
        id = id,
        displayName = displayName,
        status = status.name,
        business = business,
        country = country,
        businessId = businessId,
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

    fun createPaymentMethod(
        token: String
    ) = PaymentMethod(
        token = token,
        provider = createPaymentProvider(),
        ownerName = "Ray Sponsible",
        number = "+237670000010",
        type = PaymentMethodType.MOBILE_MONEY.name,
        status = PaymentMethodStatus.ACTIVE.name,
        accountId = 111L,
        country = "CM"
    )

    fun createPaymentMethodSummary(
        token: String
    ) = PaymentMethodSummary(
        token = token,
        provider = createPaymentProvider(),
        number = "+237670000010",
        type = PaymentMethodType.MOBILE_MONEY.name,
        status = PaymentMethodStatus.ACTIVE.name,
        accountId = 111L
    )

    fun createBusiness(
        id: Long,
        accountId: Long,
        balance: Long = 100000,
        currency: String = "XAF",
        country: String = "CM",
        status: BusinessStatus = BusinessStatus.ACTIVE
    ) = Business(
        id = id,
        balance = balance,
        currency = currency,
        country = country,
        status = status.name,
        accountId = accountId
    )
}
