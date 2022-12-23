package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.SearchSalesKpiResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.SearchSalesKpiRequest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchSalesKpiControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    @Test
    public fun invoke() {
        // GIVEN
        val kpis = listOf(
            Fixtures.createSalesKpiSummary(date = LocalDate.now()),
            Fixtures.createSalesKpiSummary(date = LocalDate.now().minusDays(1)),
            Fixtures.createSalesKpiSummary(date = LocalDate.now().minusDays(3)),
        )
        doReturn(SearchSalesKpiResponse(kpis)).whenever(checkoutAccess).searchSalesKpi(any())

        // GIVEN
        val request = SearchSalesKpiRequest(
            businessId = 1,
            productId = 2,
            fromDate = LocalDate.now().minusDays(28),
            toDate = LocalDate.now(),
            aggregate = true,
        )
        val response = rest.postForEntity(url(), request, SearchSalesKpiResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).searchSalesKpi(
            request = com.wutsi.checkout.access.dto.SearchSalesKpiRequest(
                businessId = request.businessId,
                productId = request.productId,
                fromDate = request.fromDate,
                toDate = request.toDate,
                aggregate = request.aggregate,
            ),
        )

        assertEquals(response.body!!.kpis.size, kpis.size)
    }

    private fun url() = "http://localhost:$port/v1/kpis/sales/search"
}
