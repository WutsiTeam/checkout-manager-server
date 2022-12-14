package com.wutsi.checkout.manager.job

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.access.dto.GetTransactionResponse
import com.wutsi.checkout.access.dto.SearchTransactionResponse
import com.wutsi.checkout.access.dto.SyncTransactionStatusResponse
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.ProductType
import com.wutsi.enums.TransactionType
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.marketplace.access.dto.SearchProductResponse
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.GetAccountDeviceResponse
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingService
import com.wutsi.platform.core.messaging.MessagingServiceProvider
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.platform.payment.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PendingTransactionJobTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var membershipMemberApi: MembershipAccessApi

    @MockBean
    private lateinit var marketplaceAccessApi: MarketplaceAccessApi

    @MockBean
    protected lateinit var messagingServiceProvider: MessagingServiceProvider

    protected lateinit var mail: MessagingService

    protected lateinit var push: MessagingService

    @Autowired
    protected lateinit var job: PendingTransactionJob

    private val device = Fixtures.createDevice()

    private val customerId = 1L
    private val merchantId = 333L
    private val businessId = 555L

    @BeforeEach
    fun setUp() {
        mail = mock()
        push = mock()
        doReturn(mail).whenever(messagingServiceProvider).get(MessagingType.EMAIL)
        doReturn(UUID.randomUUID().toString()).whenever(mail).send(any())

        doReturn(push).whenever(messagingServiceProvider).get(MessagingType.PUSH_NOTIFICATION)
        doReturn(UUID.randomUUID().toString()).whenever(push).send(any())

        val txs = listOf(
            Fixtures.createTransactionSummary(
                "1",
                type = TransactionType.CHARGE,
                orderId = "111",
                status = Status.PENDING
            ),
            Fixtures.createTransactionSummary(
                "2",
                type = TransactionType.CHARGE,
                orderId = "222",
                status = Status.PENDING
            )
        )
        doReturn(SearchTransactionResponse(txs)).whenever(checkoutAccessApi).searchTransaction(any())

        doReturn(SyncTransactionStatusResponse("1", Status.SUCCESSFUL.name)).whenever(checkoutAccessApi)
            .syncTransactionStatus("1")
        doReturn(SyncTransactionStatusResponse("2", Status.PENDING.name)).whenever(checkoutAccessApi)
            .syncTransactionStatus("2")

        val tx = Fixtures.createTransaction(
            txs[0].id,
            type = TransactionType.CHARGE,
            status = Status.SUCCESSFUL,
            orderId = txs[0].orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(txs[0].id)

        val merchant = Fixtures.createAccount(
            id = merchantId,
            businessId = businessId,
            displayName = "House of Pleasure",
            email = "house.of.plaesure@gmail.com"
        )
        doReturn(GetAccountResponse(merchant)).whenever(membershipMemberApi).getAccount(any())
        doReturn(GetAccountDeviceResponse(device)).whenever(membershipMemberApi).getAccountDevice(any())
    }

    @Test
    fun pendingCharges() {
        // GIVEN
        val order = Fixtures.createOrder(
            id = "111111",
            status = OrderStatus.UNKNOWN,
            businessId = businessId,
            accountId = customerId
        )
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(any())

        val prod = Fixtures.createProductSummary(
            id = order.items[0].productId,
            type = ProductType.PHYSICAL_PRODUCT
        )
        doReturn(SearchProductResponse(listOf(prod))).whenever(marketplaceAccessApi).searchProduct(any())

        // WHEN
        job.run()
        Thread.sleep(10000)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus("111", UpdateOrderStatusRequest(OrderStatus.OPENED.name))
        verify(checkoutAccessApi, never()).updateOrderStatus(eq("222"), any())

        // Email notification
        Thread.sleep(20000)
        val email = argumentCaptor<Message>()
        verify(mail, times(2)).send(email.capture())

        val pushNotification = argumentCaptor<Message>()
        verify(push).send(pushNotification.capture())
        assertEquals(device.token, pushNotification.firstValue.recipient.deviceToken)
    }

    @Test
    fun pendingChargesWithEvent() {
        // GIVEN
        val order = Fixtures.createOrder(
            id = "111111",
            status = OrderStatus.UNKNOWN,
            businessId = 5555,
            accountId = 1
        )
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(any())

        val prod = Fixtures.createProductSummary(
            id = order.items[0].productId,
            type = ProductType.EVENT,
            thumbnailUrl = "https://afrikinfo.net/wp-content/uploads/2022/11/IMG-20221111-WA0003.jpg"
        )
        doReturn(SearchProductResponse(listOf(prod))).whenever(marketplaceAccessApi).searchProduct(any())

        // WHEN
        job.run()
        Thread.sleep(10000)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus("111", UpdateOrderStatusRequest(OrderStatus.OPENED.name))
        verify(checkoutAccessApi, never()).updateOrderStatus(eq("222"), any())

        // Email notification
        Thread.sleep(20000)
        val email = argumentCaptor<Message>()
        verify(mail, times(2)).send(email.capture())

        val pushNotification = argumentCaptor<Message>()
        verify(push).send(pushNotification.capture())
        assertEquals(device.token, pushNotification.firstValue.recipient.deviceToken)
    }
}
