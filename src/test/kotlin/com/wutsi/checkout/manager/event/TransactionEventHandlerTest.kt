package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.access.dto.GetTransactionResponse
import com.wutsi.checkout.access.dto.SyncTransactionStatusResponse
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.TransactionType
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.nio.charset.Charset
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class TransactionEventHandlerTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var eventStream: EventStream

    @Autowired
    private lateinit var handler: TransactionEventHandler

    val transactionId = "3333"
    val orderId = "1111"
    val payload = TransactionEventPayload(
        transactionId = transactionId
    )

    val event = Event(
        payload = ObjectMapper().writeValueAsString(payload),
        timestamp = OffsetDateTime.now()
    )

    @Test
    fun pendingToSuccess() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.PENDING,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        val order = Fixtures.createOrder(id = orderId, status = OrderStatus.UNKNOWN)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(orderId)

        doReturn(SyncTransactionStatusResponse(status = Status.SUCCESSFUL.name)).whenever(checkoutAccessApi)
            .syncTransactionStatus(payload.transactionId)

        // WHEN
        handler.onTransactionPending(event)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus(orderId, UpdateOrderStatusRequest(OrderStatus.OPENED.name))

        verify(eventStream).publish(
            EventURN.ORDER_OPENED.urn,
            OrderEventPayload(orderId = orderId)
        )
    }

    @Test
    fun pendingToPending() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.PENDING,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        val order = Fixtures.createOrder(id = orderId, status = OrderStatus.UNKNOWN)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(orderId)

        doReturn(SyncTransactionStatusResponse(status = Status.PENDING.name)).whenever(checkoutAccessApi)
            .syncTransactionStatus(payload.transactionId)

        // WHEN
        handler.onTransactionPending(event)

        // THEN
        verify(checkoutAccessApi, never()).updateOrderStatus(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun orderAlreadyProcessed() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.PENDING,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        val order = Fixtures.createOrder(id = orderId, status = OrderStatus.OPENED)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(orderId)

        doReturn(SyncTransactionStatusResponse(status = Status.SUCCESSFUL.name)).whenever(checkoutAccessApi)
            .syncTransactionStatus(payload.transactionId)

        // WHEN
        handler.onTransactionPending(event)

        // THEN
        verify(checkoutAccessApi, never()).updateOrderStatus(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun pendingToTransactionFailure() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.PENDING,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        val order = Fixtures.createOrder(id = orderId, status = OrderStatus.UNKNOWN)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(orderId)

        val ex = createFeignConflictException(errorCode = ErrorURN.TRANSACTION_FAILED.urn)
        doThrow(ex).whenever(checkoutAccessApi)
            .syncTransactionStatus(payload.transactionId)

        // WHEN
        handler.onTransactionPending(event)

        // THEN
        verify(checkoutAccessApi, never()).updateOrderStatus(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun pendingToUnexpectedError() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.PENDING,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        val order = Fixtures.createOrder(id = orderId, status = OrderStatus.UNKNOWN)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(orderId)

        val ex = createFeignConflictException(errorCode = "xxxx")
        doThrow(ex).whenever(checkoutAccessApi)
            .syncTransactionStatus(payload.transactionId)

        // WHEN
        assertThrows<FeignException> {
            handler.onTransactionPending(event)
        }

        // THEN
        verify(checkoutAccessApi, never()).updateOrderStatus(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun successful() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.SUCCESSFUL,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        // WHEN
        handler.onTransactionPending(event)

        // THEN
        verify(checkoutAccessApi, never()).syncTransactionStatus(any())
        verify(checkoutAccessApi, never()).updateOrderStatus(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun failed() {
        // GIVEN
        val tx = Fixtures.createTransaction(
            transactionId,
            TransactionType.CHARGE,
            Status.FAILED,
            orderId = orderId
        )
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccessApi).getTransaction(transactionId)

        // WHEN
        handler.onTransactionPending(event)

        // THEN
        verify(checkoutAccessApi, never()).syncTransactionStatus(any())
        verify(checkoutAccessApi, never()).updateOrderStatus(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    private fun createFeignConflictException(
        errorCode: String
    ) = FeignException.Conflict(
        "",
        Request.create(
            Request.HttpMethod.POST,
            "https://www.google.ca",
            emptyMap(),
            "".toByteArray(),
            Charset.defaultCharset(),
            RequestTemplate()
        ),
        """
            {
                "error":{
                    "code": "$errorCode"
                }
            }
        """.trimIndent().toByteArray(),
        emptyMap()
    )
}
