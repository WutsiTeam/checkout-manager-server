package com.wutsi.checkout.manager

import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.BusinessSummary
import com.wutsi.checkout.access.dto.CreateCashoutResponse
import com.wutsi.checkout.access.dto.CreateChargeResponse
import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.access.dto.OrderItem
import com.wutsi.checkout.access.dto.OrderSummary
import com.wutsi.checkout.access.dto.PaymentMethod
import com.wutsi.checkout.access.dto.PaymentMethodSummary
import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.checkout.access.dto.SalesKpiSummary
import com.wutsi.checkout.access.dto.Transaction
import com.wutsi.checkout.access.dto.TransactionSummary
import com.wutsi.enums.AccountStatus
import com.wutsi.enums.BusinessStatus
import com.wutsi.enums.ChannelType
import com.wutsi.enums.DeviceType
import com.wutsi.enums.DiscountType
import com.wutsi.enums.MeetingProviderType
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.enums.ProductStatus
import com.wutsi.enums.ProductType
import com.wutsi.enums.TransactionType
import com.wutsi.marketplace.access.dto.CancellationPolicy
import com.wutsi.marketplace.access.dto.CategorySummary
import com.wutsi.marketplace.access.dto.DiscountSummary
import com.wutsi.marketplace.access.dto.Event
import com.wutsi.marketplace.access.dto.FileSummary
import com.wutsi.marketplace.access.dto.MeetingProviderSummary
import com.wutsi.marketplace.access.dto.Offer
import com.wutsi.marketplace.access.dto.OfferPrice
import com.wutsi.marketplace.access.dto.OfferSummary
import com.wutsi.marketplace.access.dto.PictureSummary
import com.wutsi.marketplace.access.dto.Product
import com.wutsi.marketplace.access.dto.ProductSummary
import com.wutsi.marketplace.access.dto.ReservationSummary
import com.wutsi.marketplace.access.dto.ReturnPolicy
import com.wutsi.marketplace.access.dto.Store
import com.wutsi.marketplace.access.dto.StoreSummary
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.access.dto.Category
import com.wutsi.membership.access.dto.Device
import com.wutsi.membership.access.dto.Phone
import com.wutsi.membership.access.dto.Place
import com.wutsi.platform.payment.GatewayType
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object Fixtures {
    fun createAccount(
        id: Long = System.currentTimeMillis(),
        status: AccountStatus = AccountStatus.ACTIVE,
        business: Boolean = false,
        businessId: Long? = null,
        storeId: Long? = null,
        country: String = "CM",
        phoneNumber: String = "+237670000010",
        displayName: String = "Ray Sponsible",
        email: String? = null,
        pictureUrl: String = "https://ik.imagekit.io/cx8qxsgz4d/user/12/picture/tr:w-64,h-64,fo-face/023bb5c8-7b09-4f2f-be51-29f5c851c2c0-scaled_image_picker1721723356188894418.png",
        language: String = "fr",
    ) = Account(
        id = id,
        displayName = displayName,
        status = status.name,
        business = business,
        country = country,
        businessId = businessId,
        storeId = storeId,
        email = email,
        phone = Phone(
            number = phoneNumber,
            country = country,
        ),
        pictureUrl = pictureUrl,
        category = Category(
            id = 100L,
            title = "Art",
        ),
        city = Place(
            id = 1000L,
            name = "Douala",
            longName = "Douala, Cameroun",
        ),
        facebookId = "google",
        twitterId = "google",
        instagramId = "google",
        youtubeId = "google",
        website = "http://www.google.com",
        whatsapp = true,
        language = language,
    )

    fun createPaymentProvider(
        id: Long = System.currentTimeMillis(),
        type: PaymentMethodType = PaymentMethodType.MOBILE_MONEY,
        code: String = "MTN",
    ) = PaymentProviderSummary(
        id = id,
        code = code,
        type = type.name,
        logoUrl = "https://prod-wutsi.s3.amazonaws.com/static/wutsi-assets/images/payment-providers/mtn.png",
    )

    fun createPaymentMethod(
        token: String,
        accountId: Long = -1,
    ) = PaymentMethod(
        token = token,
        provider = createPaymentProvider(),
        ownerName = "Ray Sponsible",
        number = "+237670000010",
        type = PaymentMethodType.MOBILE_MONEY.name,
        status = PaymentMethodStatus.ACTIVE.name,
        accountId = accountId,
        country = "CM",
    )

    fun createPaymentMethodSummary(
        token: String,
    ) = PaymentMethodSummary(
        token = token,
        provider = createPaymentProvider(),
        number = "+237670000010",
        type = PaymentMethodType.MOBILE_MONEY.name,
        status = PaymentMethodStatus.ACTIVE.name,
        accountId = 111L,
        ownerName = "Ray Sponsible",
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
    )

    fun createBusiness(
        id: Long,
        accountId: Long,
        balance: Long = 100000,
        currency: String = "XAF",
        country: String = "CM",
        status: BusinessStatus = BusinessStatus.ACTIVE,
    ) = Business(
        id = id,
        balance = balance,
        currency = currency,
        country = country,
        status = status.name,
        accountId = accountId,
        totalOrders = 500,
        totalSales = 120000,
        cashoutBalance = (balance * .75).toLong(),
    )

    fun createBusinessSummary(
        id: Long,
        accountId: Long,
        balance: Long = 100000,
        currency: String = "XAF",
        country: String = "CM",
        status: BusinessStatus = BusinessStatus.ACTIVE,
    ) = BusinessSummary(
        id = id,
        balance = balance,
        currency = currency,
        country = country,
        status = status.name,
        accountId = accountId,
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
    )

    fun createOrder(
        id: String,
        businessId: Long = -1,
        accountId: Long = -1,
        subTotalPrice: Long = 10000,
        totalDiscounts: Long = 1000,
        status: OrderStatus = OrderStatus.UNKNOWN,
        items: List<OrderItem>? = null,
    ) = Order(
        id = id,
        shortId = id.takeLast(4),
        business = createBusinessSummary(businessId, accountId),
        totalPrice = subTotalPrice - totalDiscounts,
        totalDiscount = totalDiscounts,
        subTotalPrice = subTotalPrice,
        totalPaid = subTotalPrice,
        balance = 0,
        status = status.name,
        customerName = "Ray Sponsible",
        customerEmail = "ray.sponsible@gmail.com",
        deviceType = DeviceType.MOBILE.name,
        channelType = ChannelType.WEB.name,
        currency = "XAF",
        notes = "Yo man",
        deviceId = "4309403-43094039-43094309",
        items = items ?: listOf(
            OrderItem(
                productId = 1,
                productType = ProductType.PHYSICAL_PRODUCT.name,
                quantity = 1,
                title = "Product #1",
                pictureUrl = "https://img.com/1.png",
                totalDiscount = 100,
                unitPrice = 2000,
                subTotalPrice = 2000,
                totalPrice = 2000,
            ),
            OrderItem(
                productId = 2,
                productType = ProductType.PHYSICAL_PRODUCT.name,
                quantity = 2,
                title = "Product #2",
                pictureUrl = "https://img.com/2.png",
                totalDiscount = 0,
                unitPrice = 2000,
                subTotalPrice = 4000,
                totalPrice = 4000,
            ),
        ),
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        updated = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        expires = OffsetDateTime.of(2100, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        transactions = listOf(
            createTransactionSummary(
                id = "111",
                type = TransactionType.CHARGE,
                status = Status.SUCCESSFUL,
                amount = subTotalPrice - totalDiscounts,
            ),
        ),
    )

    fun createOrderSummary(id: String) = OrderSummary(
        id = id,
    )

    fun createChargeResponse(status: Status = Status.PENDING) =
        CreateChargeResponse(transactionId = UUID.randomUUID().toString(), status = status.name)

    fun createCashoutResponse(status: Status = Status.PENDING) =
        CreateCashoutResponse(transactionId = UUID.randomUUID().toString(), status = status.name)

    fun createStoreSummary(
        id: Long = -1,
    ) = StoreSummary(
        id = id,
        currency = "XAF",
    )

    fun createStore(id: Long = -1) = Store(
        id = id,
        currency = "XAF",
        returnPolicy = ReturnPolicy(
            accepted = true,
            contactWindow = 24,
            shipBackWindow = 72,
            message = "This is the return policy message",
        ),
        cancellationPolicy = CancellationPolicy(
            accepted = true,
            window = 24,
            message = "This is the cancellation policy message",
        ),
    )

    fun createProduct(
        id: Long = -1,
        storeId: Long = -1,
        quantity: Int? = 11,
        pictures: List<PictureSummary> = emptyList(),
        type: ProductType = ProductType.PHYSICAL_PRODUCT,
        event: Event? = null,
        files: List<FileSummary> = emptyList(),
    ) = Product(
        id = id,
        store = Fixtures.createStoreSummary(storeId),
        pictures = pictures,
        summary = "This is a summary",
        description = "This is the description",
        price = 100000L,
        quantity = quantity,
        status = ProductStatus.DRAFT.name,
        thumbnail = if (pictures.isEmpty()) null else pictures[0],
        currency = "XAF",
        title = "This is the title",
        category = CategorySummary(
            id = 1,
            title = "Art",
        ),
        type = type.name,
        event = event,
        files = files,
    )

    fun createEvent() = Event(
        online = true,
        meetingPassword = "123456",
        meetingId = "1234567890",
        meetingJoinUrl = "https://us04.zoom.us/j/12345678",
        starts = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        ends = OffsetDateTime.of(2020, 1, 1, 15, 30, 0, 0, ZoneOffset.UTC),
        meetingProvider = MeetingProviderSummary(
            id = 1000,
            type = MeetingProviderType.ZOOM.name,
            name = "Zoom",
            logoUrl = "https://prod-wutsi.s3.amazonaws.com/static/wutsi-assets/images/meeting-providers/zoom.png",
        ),
    )

    fun createFileSummary(
        id: Long,
        name: String = "$id.png",
        url: String = "https://www.img.com/$id.png",
    ) = FileSummary(
        id = id,
        url = url,
        name = name,
        contentType = "image/png",
        contentSize = 12000,
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
    )

    fun createProductSummary(
        id: Long = -1,
        storeId: Long = -1,
        quantity: Int? = 11,
        type: ProductType = ProductType.PHYSICAL_PRODUCT,
        thumbnailUrl: String? = null,
        event: Event? = null,
    ) = ProductSummary(
        id = id,
        storeId = storeId,
        summary = "This is a summary",
        price = 100000L,
        quantity = quantity,
        status = ProductStatus.DRAFT.name,
        currency = "XAF",
        title = "This is the title #$id",
        type = type.name,
        thumbnailUrl = thumbnailUrl,
        event = event,
    )

    fun createPictureSummary(id: Long = -1) = PictureSummary(
        id = id,
        url = "https://img.com/$id.png",
    )

    fun createTransaction(
        id: String,
        type: TransactionType,
        status: Status,
        orderId: String? = null,
        businessId: Long = -1,
        accountId: Long = -1,
    ) = Transaction(
        id = id,
        type = type.name,
        orderId = orderId,
        status = status.name,
        description = "This is description",
        currency = "XAF",
        business = createBusinessSummary(businessId, accountId),
        email = "ray.sponsble@gmail.com",
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        updated = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        amount = 10500,
        errorCode = ErrorCode.APPROVAL_REJECTED.name,
        customerAccountId = 1111L,
        paymentMethod = Fixtures.createPaymentMethodSummary(""),
        financialTransactionId = "1111-111",
        gatewayTransactionId = "2222-222",
        supplierErrorCode = "xyz",
        net = 10000,
        fees = 500,
        gatewayFees = 250,
        gatewayType = GatewayType.FLUTTERWAVE.name,
    )

    fun createTransactionSummary(
        id: String,
        type: TransactionType = TransactionType.CHARGE,
        status: Status = Status.SUCCESSFUL,
        orderId: String? = null,
        amount: Long = 10000,
    ) = TransactionSummary(
        id = id,
        type = type.name,
        orderId = orderId,
        status = status.name,
        amount = amount,
        paymentMethod = createPaymentMethodSummary("xxxx"),
        created = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
        updated = OffsetDateTime.of(2020, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC),
    )

    fun createDevice() = Device(
        token = UUID.randomUUID().toString(),
    )

    fun createSalesKpiSummary(date: LocalDate = LocalDate.now()) = SalesKpiSummary(
        date = date,
        totalOrders = 100,
        totalValue = 250000,
        totalUnits = 5000,
        totalViews = 300000,
    )

    fun createOfferPrice(productId: Long, discountId: Long? = null, savings: Long = 0) = OfferPrice(
        productId = productId,
        discountId = discountId,
        savings = savings,
    )

    fun createOfferSummary(
        product: ProductSummary,
        price: OfferPrice,
    ) = OfferSummary(
        product = product,
        price = price,
    )

    fun createOffer(
        product: Product,
        price: OfferPrice,
    ) = Offer(
        product = product,
        price = price,
    )

    fun createDiscountSummary(id: Long) = DiscountSummary(
        id = id,
        name = "FIN25",
        rate = 25,
        type = DiscountType.SALES.name,
    )

    fun createReservationSummary(id: Long) = ReservationSummary(
        id = id,
    )
}
