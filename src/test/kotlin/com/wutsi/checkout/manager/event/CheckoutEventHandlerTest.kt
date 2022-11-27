package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.ReservationStatus
import com.wutsi.event.CheckoutEventPayload
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.marketplace.access.dto.UpdateReservationStatusRequest
import com.wutsi.platform.core.stream.Event
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CheckoutEventHandlerTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var marketplaceAccessApi: MarketplaceAccessApi

    @Autowired
    private lateinit var handler: CheckoutEventHandler

    val payload = CheckoutEventPayload(
        orderId = "1111",
        reservationId = 111L
    )

    val event = Event(
        payload = ObjectMapper().writeValueAsString(payload),
        timestamp = OffsetDateTime.now()
    )

    @Test
    fun onChargeFailed() {
        // WHEN
        handler.onChargeFailed(event)

        // THEN
        verify(marketplaceAccessApi).updateReservationStatus(
            id = payload.reservationId!!,
            request = UpdateReservationStatusRequest(
                status = ReservationStatus.CANCELLED.name
            )
        )
    }

    @Test
    fun onChargeSuccessful() {
        // WHEN
        handler.onChargeSuccessful(event)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus(
            id = payload.orderId,
            request = UpdateOrderStatusRequest(
                status = OrderStatus.OPENED.name
            )
        )
    }
}
