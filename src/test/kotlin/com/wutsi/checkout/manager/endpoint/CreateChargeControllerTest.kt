package com.wutsi.checkout.manager.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetBusinessResponse
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.CreateChargeRequest
import com.wutsi.checkout.manager.dto.CreateChargeResponse
import com.wutsi.enums.PaymentMethodType
import com.wutsi.marketplace.access.dto.CreateReservationResponse
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.payment.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateChargeControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    private val businessAccountId = 33333L
    private val reservationId = 11L
    private val orderId = "1111"
    private var order = Fixtures.createOrder(id = orderId, subTotalPrice = 15000)
    private val businessAccount =
        Fixtures.createAccount(id = businessAccountId, businessId = BUSINESS_ID, business = true)
    private val business = Fixtures.createBusiness(id = BUSINESS_ID, accountId = businessAccountId)
    private val request = CreateChargeRequest(
        businessId = BUSINESS_ID,
        idempotencyKey = UUID.randomUUID().toString(),
        paymentProviderId = 1000L,
        paymentMethodToken = null,
        paymenMethodNumber = "+237670000010",
        paymentMethodType = PaymentMethodType.MOBILE_MONEY.name,
        orderId = orderId,
        description = "This is nice",
        email = "ray.sponsible@gmail.com",
        paymentMethodOwnerName = "Ray Sponsible",
    )

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(GetAccountResponse(businessAccount)).whenever(membershipAccess).getAccount(businessAccountId)

        doReturn(CreateReservationResponse(reservationId)).whenever(marketplaceAccessApi).createReservation(any())

        doReturn(GetBusinessResponse(business)).whenever(checkoutAccess).getBusiness(BUSINESS_ID)

        doReturn(GetOrderResponse(order)).whenever(checkoutAccess).getOrder(orderId)
    }

    @Test
    fun pending() {
        // GIVEN
        val transactionResponse = Fixtures.createChargeResponse(status = Status.PENDING)
        doReturn(transactionResponse).whenever(checkoutAccess).createCharge(any())

        // WHEN
        val response = rest.postForEntity(url(), request, CreateChargeResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createCharge(
            request = com.wutsi.checkout.access.dto.CreateChargeRequest(
                businessId = business.id,
                orderId = request.orderId,
                paymentMethodOwnerName = request.paymentMethodOwnerName,
                paymentProviderId = request.paymentProviderId,
                amount = order.balance,
                paymentMethodToken = request.paymentMethodToken,
                idempotencyKey = request.idempotencyKey,
                paymenMethodNumber = request.paymenMethodNumber,
                email = request.email,
                paymentMethodType = request.paymentMethodType,
                description = request.description,
            ),
        )

        val result = response.body!!
        assertEquals(transactionResponse.transactionId, result.transactionId)
        assertEquals(transactionResponse.status, result.status)

        verify(eventStream, never()).enqueue(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun success() {
        // GIVEN
        val transactionResponse = Fixtures.createChargeResponse(status = Status.SUCCESSFUL)
        doReturn(transactionResponse).whenever(checkoutAccess).createCharge(any())

        // WHEN
        val response = rest.postForEntity(url(), request, CreateChargeResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createCharge(
            request = com.wutsi.checkout.access.dto.CreateChargeRequest(
                businessId = business.id,
                orderId = request.orderId,
                paymentMethodOwnerName = request.paymentMethodOwnerName,
                paymentProviderId = request.paymentProviderId,
                amount = order.balance,
                paymentMethodToken = request.paymentMethodToken,
                idempotencyKey = request.idempotencyKey,
                paymenMethodNumber = request.paymenMethodNumber,
                email = request.email,
                paymentMethodType = request.paymentMethodType,
                description = request.description,
            ),
        )

        val result = response.body!!
        assertEquals(transactionResponse.transactionId, result.transactionId)
        assertEquals(transactionResponse.status, result.status)

//        verify(eventStream).enqueue(
//            InternalEventURN.TRANSACTION_SUCCESSFUL.urn,
//            TransactionEventPayload(transactionResponse.transactionId),
//        )
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun `charge error`() {
        // GIVEN
        val cause = createFeignConflictException(ErrorURN.TRANSACTION_FAILED.urn)
        doThrow(cause).whenever(checkoutAccess).createCharge(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CreateChargeResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(eventStream, never()).publish(any(), any())

        val response = ObjectMapper().readValue(ex.responseBodyAsString, ErrorResponse::class.java)
        assertEquals(com.wutsi.error.ErrorURN.TRANSACTION_FAILED.urn, response.error.code)
    }

    private fun url(): String = "http://localhost:$port/v1/transactions/charge"
}
