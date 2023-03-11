package com.wutsi.checkout.manager.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetBusinessResponse
import com.wutsi.checkout.access.dto.GetPaymentMethodResponse
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.CreateCashoutRequest
import com.wutsi.checkout.manager.dto.CreateCashoutResponse
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
class CreateCashoutControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    private val account =
        Fixtures.createAccount(
            id = ACCOUNT_ID,
            businessId = BUSINESS_ID,
            business = true,
            email = "yo@gmail.com",
        )
    private val business = Fixtures.createBusiness(id = BUSINESS_ID, accountId = ACCOUNT_ID)
    private val paymentMethod = Fixtures.createPaymentMethod(token = "11111", accountId = BUSINESS_ID)
    private val request = CreateCashoutRequest(
        paymentMethodToken = paymentMethod.token,
        idempotencyKey = UUID.randomUUID().toString(),
        description = "This is nice",
        amount = 15000,
    )

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(GetAccountResponse(account)).whenever(membershipAccess).getAccount(ACCOUNT_ID)
        doReturn(GetPaymentMethodResponse(paymentMethod)).whenever(checkoutAccess).getPaymentMethod(paymentMethod.token)
        doReturn(GetBusinessResponse(business)).whenever(checkoutAccess).getBusiness(BUSINESS_ID)
    }

    @Test
    fun pending() {
        // GIVEN
        val transactionResponse = Fixtures.createCashoutResponse(status = Status.PENDING)
        doReturn(transactionResponse).whenever(checkoutAccess).createCashout(any())

        // WHEN
        val response = rest.postForEntity(url(), request, CreateCashoutResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createCashout(
            request = com.wutsi.checkout.access.dto.CreateCashoutRequest(
                businessId = business.id,
                amount = request.amount,
                paymentMethodToken = request.paymentMethodToken,
                idempotencyKey = request.idempotencyKey,
                email = account.email!!,
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
        val transactionResponse = Fixtures.createCashoutResponse(status = Status.SUCCESSFUL)
        doReturn(transactionResponse).whenever(checkoutAccess).createCashout(any())

        // WHEN
        val response = rest.postForEntity(url(), request, CreateCashoutResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createCashout(
            request = com.wutsi.checkout.access.dto.CreateCashoutRequest(
                businessId = business.id,
                amount = request.amount,
                paymentMethodToken = request.paymentMethodToken,
                idempotencyKey = request.idempotencyKey,
                email = account.email!!,
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
    fun error() {
        // GIVEN
        val cause = createFeignConflictException(ErrorURN.TRANSACTION_FAILED.urn)
        doThrow(cause).whenever(checkoutAccess).createCashout(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CreateCashoutResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(eventStream, never()).publish(any(), any())

        val response = ObjectMapper().readValue(ex.responseBodyAsString, ErrorResponse::class.java)
        assertEquals(com.wutsi.error.ErrorURN.TRANSACTION_FAILED.urn, response.error.code)
    }

    private fun url(): String = "http://localhost:$port/v1/transactions/cashout"
}
