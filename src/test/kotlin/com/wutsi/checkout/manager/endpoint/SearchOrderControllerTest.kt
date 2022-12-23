package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.SearchOrderRequest
import com.wutsi.enums.OrderStatus
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchOrderControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    @Test
    public fun search() {
        // GIVEN
        val orders = listOf(
            Fixtures.createOrderSummary("1"),
            Fixtures.createOrderSummary("2"),
        )
        doReturn(com.wutsi.checkout.access.dto.SearchOrderResponse(orders)).whenever(checkoutAccess).searchOrder(any())

        // WHEN
        val request = SearchOrderRequest(
            customerAccountId = 222,
            limit = 1000,
            offset = 111,
            expiresTo = OffsetDateTime.of(2020, 1, 10, 10, 30, 0, 0, ZoneOffset.UTC),
            createdFrom = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
            createdTo = OffsetDateTime.of(2020, 1, 30, 0, 0, 0, 0, ZoneOffset.UTC),
            status = listOf(OrderStatus.EXPIRED.name, OrderStatus.COMPLETED.name),
            businessId = 111,
            productId = 333L,
        )
        val response =
            rest.postForEntity(url(), request, com.wutsi.checkout.manager.dto.SearchOrderResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).searchOrder(
            request = com.wutsi.checkout.access.dto.SearchOrderRequest(
                customerAccountId = request.customerAccountId,
                limit = request.limit,
                offset = request.offset,
                expiresTo = request.expiresTo,
                createdTo = request.createdTo,
                createdFrom = request.createdFrom,
                businessId = request.businessId,
                status = request.status,
                productId = request.productId,
            ),
        )

        val result = response.body!!.orders
        assertEquals(orders.size, result.size)
    }

    private fun url() = "http://localhost:$port/v1/orders/search"
}
