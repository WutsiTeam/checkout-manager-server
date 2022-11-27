package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.ReservationStatus
import com.wutsi.event.CheckoutEventPayload
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.marketplace.access.dto.UpdateReservationStatusRequest
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import org.springframework.stereotype.Service

@Service
class CheckoutEventHandler(
    private val mapper: ObjectMapper,
    private val logger: KVLogger,
    private val marketplaceAccessApi: MarketplaceAccessApi,
    private val checkoutAccessApi: CheckoutAccessApi
) {
    fun onChargeFailed(event: Event) {
        val payload = toCheckoutEventPayload(event)
        log(payload)

        payload.reservationId?.let {
            marketplaceAccessApi.updateReservationStatus(
                id = it,
                request = UpdateReservationStatusRequest(
                    status = ReservationStatus.CANCELLED.name
                )
            )
        }
    }

    fun onChargeSuccessful(event: Event) {
        val payload = toCheckoutEventPayload(event)
        log(payload)

        checkoutAccessApi.updateOrderStatus(
            id = payload.orderId,
            request = UpdateOrderStatusRequest(
                status = OrderStatus.OPENED.name
            )
        )
    }

    private fun toCheckoutEventPayload(event: Event): CheckoutEventPayload =
        mapper.readValue(event.payload, CheckoutEventPayload::class.java)

    private fun log(payload: CheckoutEventPayload) {
        logger.add("payload_order_id", payload.orderId)
        logger.add("payload_reservation_id", payload.reservationId)
    }
}
