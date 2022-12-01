package com.wutsi.checkout.manager

import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.CreateChargeResponse
import com.wutsi.checkout.access.dto.Discount
import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.access.dto.OrderItem
import com.wutsi.checkout.access.dto.OrderSummary
import com.wutsi.checkout.access.dto.PaymentMethod
import com.wutsi.checkout.access.dto.PaymentMethodSummary
import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.checkout.access.dto.Transaction
import com.wutsi.checkout.access.dto.TransactionSummary
import com.wutsi.enums.AccountStatus
import com.wutsi.enums.BusinessStatus
import com.wutsi.enums.ChannelType
import com.wutsi.enums.DeviceType
import com.wutsi.enums.DiscountType
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.enums.ProductStatus
import com.wutsi.enums.TransactionType
import com.wutsi.marketplace.access.dto.CategorySummary
import com.wutsi.marketplace.access.dto.PictureSummary
import com.wutsi.marketplace.access.dto.Product
import com.wutsi.marketplace.access.dto.ProductSummary
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.access.dto.Phone
import com.wutsi.platform.payment.GatewayType
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

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
        accountId = 111L,
        ownerName = "Ray Sponsible"
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

    fun createOrder(
        id: String,
        businessId: Long = -1,
        totalPrice: Long = 100000L,
        status: OrderStatus = OrderStatus.UNKNOWN
    ) = Order(
        id = id,
        businessId = businessId,
        totalPrice = totalPrice,
        balance = totalPrice,
        status = status.name,
        customerName = "Ray Sponsible",
        customerEmail = "ray.sponsible@gmail.com",
        deviceType = DeviceType.MOBILE.name,
        channelType = ChannelType.WEB.name,
        currency = "XAF",
        notes = "Yo man",
        deviceId = "4309403-43094039-43094309",
        discounts = listOf(
            Discount(
                code = "111",
                amount = 1000,
                rate = 0,
                type = DiscountType.DYNAMIC.name
            )
        ),
        items = listOf(
            OrderItem(
                productId = 999,
                quantity = 3,
                title = "This is a product",
                pictureUrl = "https://img.com/1.png",
                totalPrice = totalPrice,
                unitPrice = totalPrice / 3,
                totalDiscount = 100,
                discounts = listOf(
                    Discount(
                        code = "111",
                        amount = 1000,
                        rate = 0,
                        type = DiscountType.DYNAMIC.name
                    )
                )
            )
        ),
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        updated = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        expires = OffsetDateTime.of(2100, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC)
    )

    fun createOrderSummary(id: String) = OrderSummary(
        id = id
    )

    fun createChargeResponse(status: Status = Status.PENDING) =
        CreateChargeResponse(transactionId = UUID.randomUUID().toString(), status = status.name)

    fun createProduct(
        id: Long = -1,
        storeId: Long = -1,
        quantity: Int? = 11,
        pictures: List<PictureSummary> = emptyList()
    ) = Product(
        id = id,
        storeId = storeId,
        pictures = pictures,
        summary = "This is a summary",
        description = "This is the description",
        price = 100000L,
        comparablePrice = 150000L,
        quantity = quantity,
        status = ProductStatus.DRAFT.name,
        thumbnail = if (pictures.isEmpty()) null else pictures[0],
        currency = "XAF",
        title = "This is the title",
        category = CategorySummary(
            id = 1,
            title = "Art"
        )
    )

    fun createProductSummary(
        id: Long = -1,
        storeId: Long = -1,
        quantity: Int? = 11
    ) = ProductSummary(
        id = id,
        storeId = storeId,
        summary = "This is a summary",
        price = 100000L,
        comparablePrice = 150000L,
        quantity = quantity,
        status = ProductStatus.DRAFT.name,
        thumbnailUrl = "http://img.com/$id.png",
        currency = "XAF",
        title = "This is the title #$id"
    )

    fun createPictureSummary(id: Long = -1) = PictureSummary(
        id = id,
        url = "https://img.com/$id.png"
    )

    fun createTransaction(id: String, type: TransactionType, status: Status, orderId: String? = null) = Transaction(
        id = id,
        type = type.name,
        orderId = orderId,
        status = status.name,
        description = "This is description",
        currency = "XAF",
        businessId = 111,
        email = "ray.sponsble@gmail.com",
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        updated = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        amount = 10500,
        errorCode = ErrorCode.APPROVAL_REJECTED.name,
        customerId = 1111L,
        paymentMethod = Fixtures.createPaymentMethodSummary(""),
        financialTransactionId = "1111-111",
        gatewayTransactionId = "2222-222",
        supplierErrorCode = "xyz",
        net = 10000,
        fees = 500,
        gatewayFees = 250,
        gatewayType = GatewayType.FLUTTERWAVE.name
    )

    fun createTransactionSummary(
        id: String,
        type: TransactionType,
        status: Status = Status.SUCCESSFUL,
        orderId: String? = null
    ) = TransactionSummary(
        id = id,
        type = type.name,
        orderId = orderId,
        status = status.name
    )
}
