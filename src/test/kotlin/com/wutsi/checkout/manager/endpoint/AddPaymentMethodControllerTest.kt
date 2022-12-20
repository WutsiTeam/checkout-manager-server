package com.wutsi.checkout.manager.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.CreatePaymentMethodRequest
import com.wutsi.checkout.access.dto.CreatePaymentMethodResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodResponse
import com.wutsi.enums.AccountStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.error.ErrorURN
import com.wutsi.event.EventURN
import com.wutsi.event.PaymentMethodEventPayload
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.core.error.ErrorResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddPaymentMethodControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    private val request = AddPaymentMethodRequest(
        providerId = 111L,
        number = "30490349039",
        type = PaymentMethodType.MOBILE_MONEY.name,
        ownerName = "Ray Sponsible",
        country = "CM",
    )

    private val token = "xxx"

    @Test
    fun add() {
        // GIVEN
        val account = Fixtures.createAccount()
        doReturn(GetAccountResponse(account)).whenever(membershipAccess).getAccount(any())

        doReturn(CreatePaymentMethodResponse(token)).whenever(checkoutAccess).createPaymentMethod(any())

        // WHEN
        val response = rest.postForEntity(url(), request, AddPaymentMethodResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        val req = argumentCaptor<CreatePaymentMethodRequest>()
        verify(checkoutAccess).createPaymentMethod(req.capture())

        assertEquals(request.providerId, req.firstValue.providerId)
        assertEquals(request.number, req.firstValue.number)
        assertEquals(request.type, req.firstValue.type)
        assertEquals(request.ownerName, req.firstValue.ownerName)
        assertEquals(request.country, req.firstValue.country)

        verify(eventStream).publish(
            EventURN.PAYMENT_METHOD_ADDED.urn,
            PaymentMethodEventPayload(
                accountId = ACCOUNT_ID,
                paymentMethodToken = token,
            ),
        )
    }

    @Test
    fun notActive() {
        // GIVEN
        val account = Fixtures.createAccount(status = AccountStatus.INACTIVE)
        doReturn(GetAccountResponse(account)).whenever(membershipAccess).getAccount(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, Any::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        val response = ObjectMapper().readValue(ex.responseBodyAsString, ErrorResponse::class.java)
        assertEquals(ErrorURN.MEMBER_NOT_ACTIVE.urn, response.error.code)

        verify(checkoutAccess, never()).createPaymentMethod(any())
        verify(eventStream, never()).publish(any(), any())
    }

    private fun url() = "http://localhost:$port/v1/payment-methods"
}
