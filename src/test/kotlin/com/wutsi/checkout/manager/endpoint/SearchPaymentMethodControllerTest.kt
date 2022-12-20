package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.SearchPaymentMethodRequest
import com.wutsi.checkout.manager.dto.SearchPaymentMethodResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPaymentMethodControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun invoke() {
        // GIVEN
        val paymentMethods = listOf(
            Fixtures.createPaymentMethodSummary("111"),
            Fixtures.createPaymentMethodSummary("2222"),
        )
        doReturn(com.wutsi.checkout.access.dto.SearchPaymentMethodResponse(paymentMethods)).whenever(checkoutAccess)
            .searchPaymentMethod(
                anyOrNull(),
            )

        // THEN
        val request = SearchPaymentMethodRequest(
            status = "ACTIVE",
            limit = 1,
            offset = 100,
        )
        val response = rest.postForEntity(url(), request, SearchPaymentMethodResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).searchPaymentMethod(
            request = com.wutsi.checkout.access.dto.SearchPaymentMethodRequest(
                accountId = ACCOUNT_ID,
                status = request.status,
                limit = request.limit,
                offset = request.offset,
            ),
        )

        val payments = response.body!!.paymentMethods
        assertEquals(paymentMethods.size, payments.size)
    }

    private fun url() = "http://localhost:$port/v1/payment-methods/search"
}
