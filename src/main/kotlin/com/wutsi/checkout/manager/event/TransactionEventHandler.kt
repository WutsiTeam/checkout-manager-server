package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.workflow.HandleSuccessfulTransactionWorkflow
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class TransactionEventHandler(
    private val mapper: ObjectMapper,
    private val logger: KVLogger,
    private val workflow: HandleSuccessfulTransactionWorkflow,
) {
    fun onTransactionSuccessful(event: Event) {
        val payload = toTransactionEventPayload(event)
        log(payload)

        workflow.execute(payload.transactionId, WorkflowContext())
    }

    private fun toTransactionEventPayload(event: Event): TransactionEventPayload =
        mapper.readValue(event.payload, TransactionEventPayload::class.java)

    private fun log(payload: TransactionEventPayload) {
        logger.add("payload_transaction_id", payload.transactionId)
    }
}
