package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetPaymentMethodResponse
import com.wutsi.checkout.manager.Fixtures
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GetPaymentMethodControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    public fun invoke() {
        // GIVEN
        val paymentMethod = Fixtures.createPaymentMethod("1111")
        doReturn(GetPaymentMethodResponse(paymentMethod)).whenever(checkoutAccess).getPaymentMethod(any())

        // WHEN
        val response =
            rest.getForEntity(url("1111"), com.wutsi.checkout.manager.dto.GetPaymentMethodResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).getPaymentMethod("1111")

        val payment = response.body!!.paymentMethod
        assertEquals(paymentMethod.type, payment.type)
        assertEquals(paymentMethod.country, payment.country)
        assertEquals(paymentMethod.number, payment.number)
        assertEquals(paymentMethod.ownerName, payment.ownerName)
        assertEquals(paymentMethod.status, payment.status)
        assertEquals(paymentMethod.accountId, payment.accountId)
        assertEquals(paymentMethod.provider.code, payment.provider.code)
        assertEquals(paymentMethod.provider.type, payment.provider.type)
        assertEquals(paymentMethod.provider.name, payment.provider.name)
        assertEquals(paymentMethod.provider.logoUrl, payment.provider.logoUrl)
    }

    private fun url(token: String) = "http://localhost:$port/v1/payment-methods/$token"
}
