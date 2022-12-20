package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import com.wutsi.enums.AccountStatus
import com.wutsi.enums.OrderStatus
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.membership.access.dto.GetAccountResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdateOrderStatusControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    val account =
        Fixtures.createAccount(id = ACCOUNT_ID, business = true, businessId = 111L, status = AccountStatus.ACTIVE)
    val order = Fixtures.createOrder(id = "111", businessId = account.businessId!!, status = OrderStatus.OPENED)

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(GetAccountResponse(account)).whenever(membershipAccess).getAccount(ACCOUNT_ID)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccess).getOrder(order.id)
    }

    @Test
    public fun accept() {
        val request = UpdateOrderStatusRequest(
            orderId = order.id,
            status = OrderStatus.IN_PROGRESS.name,
            reason = "Yes!",
        )
        val response = rest.postForEntity(url(), request, Any::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).updateOrderStatus(
            id = order.id,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )

        verify(eventStream).publish(EventURN.ORDER_STARTED.urn, OrderEventPayload(order.id))
    }

    @Test
    public fun reject() {
        val request = UpdateOrderStatusRequest(
            orderId = order.id,
            status = OrderStatus.CANCELLED.name,
            reason = "Yes!",
        )
        val response = rest.postForEntity(url(), request, Any::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).updateOrderStatus(
            id = order.id,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )

        verify(eventStream).publish(EventURN.ORDER_CANCELLED.urn, OrderEventPayload(order.id))
    }

    @Test
    public fun complete() {
        val request = UpdateOrderStatusRequest(
            orderId = order.id,
            status = OrderStatus.COMPLETED.name,
            reason = "Yes!",
        )
        val response = rest.postForEntity(url(), request, Any::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).updateOrderStatus(
            id = order.id,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )

        verify(eventStream).publish(EventURN.ORDER_COMPLETED.urn, OrderEventPayload(order.id))
    }

    private fun url(): String = "http://localhost:$port/v1/orders/status"
}
