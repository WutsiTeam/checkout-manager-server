package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.stream.EventStream
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.OffsetDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class OrderEventHandlerTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var eventStream: EventStream

    @Autowired
    private lateinit var handler: OrderEventHandler

    val orderId = "1111"
    val payload = OrderEventPayload(
        orderId = orderId
    )

    val event = Event(
        payload = ObjectMapper().writeValueAsString(payload),
        timestamp = OffsetDateTime.now()
    )

    @Test
    fun expire() {
        // WHEN
        handler.onOrderExpired(event)

        // THEN
        verify(checkoutAccessApi).updateOrderStatus(orderId, UpdateOrderStatusRequest(OrderStatus.EXPIRED.name))

        verify(eventStream).publish(
            EventURN.ORDER_EXPIRED.urn,
            OrderEventPayload(orderId = orderId)
        )
    }
}
