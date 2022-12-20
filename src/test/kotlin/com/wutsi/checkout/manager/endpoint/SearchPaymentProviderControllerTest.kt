package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.SearchPaymentProviderResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.SearchPaymentProviderRequest
import com.wutsi.enums.PaymentMethodType
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPaymentProviderControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun invoke() {
        // GIVEN
        val paymentProviders = listOf(
            Fixtures.createPaymentProvider(1),
            Fixtures.createPaymentProvider(2),
        )
        doReturn(SearchPaymentProviderResponse(paymentProviders)).whenever(checkoutAccess).searchPaymentProvider(any())

        // THEN
        val request = SearchPaymentProviderRequest(
            type = PaymentMethodType.MOBILE_MONEY.name,
            number = "+237670000010",
            country = "CM",
        )
        val response = rest.postForEntity(url(), request, SearchPaymentProviderResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).searchPaymentProvider(
            request = com.wutsi.checkout.access.dto.SearchPaymentProviderRequest(
                type = request.type,
                number = request.number,
                country = request.country,
            ),
        )

        val payments = response.body!!.paymentProviders
        assertEquals(paymentProviders.size, payments.size)
    }

    private fun url() = "http://localhost:$port/v1/payment-providers/search"
}
