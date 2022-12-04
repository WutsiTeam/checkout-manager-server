package com.wutsi.checkout.manager.job

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
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
import com.wutsi.enums.TransactionType
import com.wutsi.membership.access.MembershipAccessApi
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
    protected lateinit var messagingServiceProvider: MessagingServiceProvider

    protected lateinit var mail: MessagingService

    @Autowired
    protected lateinit var job: PendingTransactionJob

    @BeforeEach
    fun setUp() {
        mail = mock()
        doReturn(mail).whenever(messagingServiceProvider).get(MessagingType.EMAIL)
        doReturn(UUID.randomUUID().toString()).whenever(mail).send(any())
    }

    @Test
    fun pendingCharges() {
        // GIVEN
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
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction("1")

        val order = Fixtures.createOrder(
            id = txs[0].orderId!!,
            status = OrderStatus.UNKNOWN,
            businessId = 555,
            accountId = 55555
        )
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(txs[0].orderId!!)

        val merchant = Fixtures.createAccount(
            id = order.business.accountId,
            businessId = order.business.id,
            displayName = "House of Pleasure"
        )
        doReturn(GetAccountResponse(merchant)).whenever(membershipMemberApi).getAccount(merchant.id)

        // WHEN
        job.run()
        Thread.sleep(10000)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus("111", UpdateOrderStatusRequest(OrderStatus.OPENED.name))
        verify(checkoutAccessApi, never()).updateOrderStatus(eq("222"), any())

        // Customer notification
        Thread.sleep(20000)
        val message = argumentCaptor<Message>()
        verify(mail).send(message.capture())
        assertEquals(order.customerEmail, message.firstValue.recipient.email)
        assertEquals(order.customerName, message.firstValue.recipient.displayName)

        // Merchant notification
    }
}
