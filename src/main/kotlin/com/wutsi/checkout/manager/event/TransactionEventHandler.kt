package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SyncTransactionStatusResponse
import com.wutsi.checkout.access.dto.Transaction
import com.wutsi.checkout.access.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.TransactionType
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
    fun onChargeSuccessful(event: Event) {
        val payload = toTransactionEventPayload(event)
        log(payload)

        payload.orderId?.let {
            checkoutAccessApi.updateOrderStatus(
                id = it,
                request = UpdateOrderStatusRequest(
                    status = OrderStatus.OPENED.name
                )
            )
        }
    }

    fun onTransactionPending(event: Event) {
        val payload = toTransactionEventPayload(event)
        log(payload)

        val tx = checkoutAccessApi.getTransaction(payload.transactionId).transaction
        if (tx.status != Status.PENDING.name) {
            logger.add("transaction_status", tx.status)
            return
        }

        try {
            val response = checkoutAccessApi.syncTransactionStatus(tx.id)
            logger.add("transaction_status", response.status)
            handleSyncResponse(tx, response)
        } catch (ex: FeignException) {
            handleSyncException(tx, ex)
        }
    }

    private fun handleSyncResponse(tx: Transaction, response: SyncTransactionStatusResponse) {
        if (response.status != Status.SUCCESSFUL.name) {
            return
        }

        if (tx.type == TransactionType.CHARGE.name) {
            tx.orderId?.let {
                eventStream.enqueue(
                    InternalEventURN.CHARGE_SUCESSFULL.urn,
                    TransactionEventPayload(transactionId = tx.id, orderId = tx.orderId)
                )
            }
        } else if (tx.type == TransactionType.CASHOUT.name) {
            eventStream.enqueue(
                InternalEventURN.CASHOUT_SUCESSFULL.urn,
                TransactionEventPayload(transactionId = tx.id)
            )
        }
    }

    private fun handleSyncException(tx: Transaction, ex: FeignException) {
        val resp = mapper.readValue(ex.contentUTF8(), ErrorResponse::class.java)
        if (resp.error.code == ErrorURN.TRANSACTION_FAILED.urn) {
            logger.add("transaction_status", Status.FAILED)
        } else {
            throw ex
        }
    }

    private fun toTransactionEventPayload(event: Event): TransactionEventPayload =
        mapper.readValue(event.payload, TransactionEventPayload::class.java)

    private fun log(payload: TransactionEventPayload) {
        logger.add("payload_transaction_id", payload.transactionId)
        logger.add("payload_order_id", payload.orderId)
    }
}
