package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import com.wutsi.enums.AccountStatus
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.ReservationStatus
import com.wutsi.marketplace.access.dto.SearchReservationResponse
import com.wutsi.marketplace.access.dto.UpdateReservationStatusRequest
import com.wutsi.membership.access.dto.GetAccountResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdateOrderStatusControllerTest : AbstractSecuredController2Test() {
    val account =
        Fixtures.createAccount(id = ACCOUNT_ID, business = true, businessId = 111L, status = AccountStatus.ACTIVE)
    val order = Fixtures.createOrder(id = "111", businessId = account.businessId!!, status = OrderStatus.OPENED)

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(ACCOUNT_ID)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccessApi).getOrder(order.id)
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

        verify(checkoutAccessApi).updateOrderStatus(
            id = order.id,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )
    }

    @Test
    public fun reject() {
        // GIVEN
        doReturn(
            SearchReservationResponse(listOf(Fixtures.createReservationSummary(id = 11L))),
        ).whenever(marketplaceAccessApi).searchReservation(any())

        // WHEN
        val request = UpdateOrderStatusRequest(
            orderId = order.id,
            status = OrderStatus.CANCELLED.name,
            reason = "Yes!",
        )
        val response = rest.postForEntity(url(), request, Any::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccessApi).updateOrderStatus(
            id = order.id,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )

        Thread.sleep(10000)
        verify(marketplaceAccessApi).updateReservationStatus(
            id = 11L,
            UpdateReservationStatusRequest(ReservationStatus.CANCELLED.name),
        )
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

        verify(checkoutAccessApi).updateOrderStatus(
            id = order.id,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )
    }

    private fun url(): String = "http://localhost:$port/v1/orders/status"
}
