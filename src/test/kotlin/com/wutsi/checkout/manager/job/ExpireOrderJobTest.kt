package com.wutsi.checkout.manager.job

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchOrderResponse
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.ReservationStatus
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.marketplace.access.dto.SearchReservationResponse
import com.wutsi.marketplace.access.dto.UpdateReservationStatusRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ExpireOrderJobTest {
    @Autowired
    private lateinit var job: ExpireOrderJob

    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var marketplaceAccessApi: MarketplaceAccessApi

    @Test
    fun run() {
        // GIVEN
        val orders = listOf(
            Fixtures.createOrderSummary("1"),
        )
        doReturn(SearchOrderResponse(orders)).whenever(checkoutAccessApi).searchOrder(any())

        val reservations = listOf(
            Fixtures.createReservationSummary(id = 1L),
        )
        doReturn(SearchReservationResponse(reservations)).whenever(marketplaceAccessApi).searchReservation(any())

        // WHEN
        job.run()

        // THEN
        verify(checkoutAccessApi).updateOrderStatus(
            orders[0].id,
            UpdateOrderStatusRequest(OrderStatus.EXPIRED.name),
        )

        Thread.sleep(10000) // Wait
        verify(marketplaceAccessApi).updateReservationStatus(
            reservations[0].id,
            UpdateReservationStatusRequest(status = ReservationStatus.CANCELLED.name),
        )
    }
}
