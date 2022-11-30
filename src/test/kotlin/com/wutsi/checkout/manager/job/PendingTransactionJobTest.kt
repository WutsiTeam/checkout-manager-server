package com.wutsi.checkout.manager.job

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
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
import com.wutsi.platform.payment.core.Status
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PendingTransactionJobTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @Autowired
    protected lateinit var job: PendingTransactionJob

    @Test
    fun pendingCharges() {
        // GIVEN
        val txs = listOf(
            Fixtures.createTransactionSummary("1", type = TransactionType.CHARGE, orderId = "111"),
            Fixtures.createTransactionSummary("2", type = TransactionType.CHARGE, orderId = "222")
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
            status = OrderStatus.UNKNOWN
        )
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(txs[0].orderId!!)

        // WHEN
        job.run()
        Thread.sleep(10000)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus("111", UpdateOrderStatusRequest(OrderStatus.OPENED.name))
        verify(checkoutAccessApi, never()).updateOrderStatus(eq("222"), any())
    }
}
