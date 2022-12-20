package com.wutsi.checkout.manager.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.manager.Fixtures
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GetOrderControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Test
    public fun invoke() {
        // GIVEN
        val orderId = "1111"
        val order = Fixtures.createOrder(id = orderId)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccess).getOrder(any())

        // WHEN
        val response = rest.getForEntity(url(orderId), com.wutsi.checkout.manager.dto.GetOrderResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        val value = response.body!!.order
        val writer = mapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(order),
            writer.writeValueAsString(value),
        )
    }

    private fun url(orderId: String) = "http://localhost:$port/v1/orders/$orderId"
}
