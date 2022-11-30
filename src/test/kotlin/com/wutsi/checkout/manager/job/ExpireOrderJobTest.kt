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
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream
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
    private lateinit var eventStream: EventStream

    @Test
    fun run() {
        // GIVEN
        val orders = listOf(
            Fixtures.createOrderSummary("1"),
            Fixtures.createOrderSummary("2")
        )
        doReturn(SearchOrderResponse(orders)).whenever(checkoutAccessApi).searchOrder(any())

        // WHEN
        job.run()

        // THEN
        verify(checkoutAccessApi).updateOrderStatus(
            orders[0].id,
            UpdateOrderStatusRequest(OrderStatus.EXPIRED.name)
        )
        verify(checkoutAccessApi).updateOrderStatus(
            orders[1].id,
            UpdateOrderStatusRequest(OrderStatus.EXPIRED.name)
        )

        verify(eventStream).publish(EventURN.ORDER_EXPIRED.urn, OrderEventPayload(orders[0].id))
        verify(eventStream).publish(EventURN.ORDER_EXPIRED.urn, OrderEventPayload(orders[1].id))
    }
}
