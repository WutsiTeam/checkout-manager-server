package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SyncTransactionStatusResponse
import com.wutsi.checkout.access.dto.Transaction
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.TransactionType
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import feign.FeignException
import org.springframework.stereotype.Service

@Service
class TransactionEventHandler(
    private val mapper: ObjectMapper,
    private val eventStream: EventStream,
    private val logger: KVLogger,
    private val checkoutAccessApi: CheckoutAccessApi
) {
    fun onTransactionPending(event: Event) {
        val payload = toTransactionEventPayload(event)
        log(payload)

        val tx = checkoutAccessApi.getTransaction(payload.transactionId).transaction
        logger.add("transaction_order_id", tx.orderId)
        logger.add("transaction_status", tx.status)
        if (tx.status != Status.PENDING.name) {
            return
        }

        try {
            val response = checkoutAccessApi.syncTransactionStatus(tx.id)
            logger.add("transaction_new_status", response.status)
            handleSyncResponse(tx, response)
        } catch (ex: FeignException) {
            handleSyncException(ex)
        }
    }

    private fun handleSyncResponse(tx: Transaction, response: SyncTransactionStatusResponse) {
        if (response.status != Status.SUCCESSFUL.name) {
            return
        }

        if (tx.type == TransactionType.CHARGE.name) {
            onChargeSuccessful(tx)
        }
    }

    private fun onChargeSuccessful(tx: Transaction) {
        val orderId = tx.orderId ?: return
        val order = checkoutAccessApi.getOrder(orderId).order
        if (order.status != OrderStatus.UNKNOWN.name) {
            return
        }
        // Open the order, since the transaction has been successful
        checkoutAccessApi.updateOrderStatus(
            id = orderId,
            request = UpdateOrderStatusRequest(
                status = OrderStatus.OPENED.name
            )
        )
        eventStream.publish(EventURN.ORDER_OPENED.urn, OrderEventPayload(orderId))
    }

    private fun handleSyncException(ex: FeignException) {
        val resp = mapper.readValue(ex.contentUTF8(), ErrorResponse::class.java)
        if (resp.error.code == ErrorURN.TRANSACTION_FAILED.urn) {
            logger.add("transaction_new_status", Status.FAILED)
        } else {
            throw ex
        }
    }

    private fun toTransactionEventPayload(event: Event): TransactionEventPayload =
        mapper.readValue(event.payload, TransactionEventPayload::class.java)

    private fun log(payload: TransactionEventPayload) {
        logger.add("payload_transaction_id", payload.transactionId)
    }
}
