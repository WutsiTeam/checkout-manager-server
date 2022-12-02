package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.dto.Transaction
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetTransactionWorkflow(
    private val objectMapper: ObjectMapper,
    private val logger: KVLogger,
    eventStream: EventStream
) : AbstractCheckoutWorkflow<String, GetTransactionResponse, Void>(eventStream) {
    companion object {
        const val DATA_SYNC = "sync"
    }

    override fun getEventType(): String? = null

    override fun toEventPayload(
        transactionId: String,
        response: GetTransactionResponse,
        context: WorkflowContext
    ): Void? = null

    override fun getValidationRules(transactionId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(transactionId: String, context: WorkflowContext): GetTransactionResponse {
        sync(transactionId, context)

        val transaction = checkoutAccessApi.getTransaction(transactionId).transaction
        /*
         * Because the order data structure is the same as the one from access layer, let use json serialization for copying the data
         */
        val json = objectMapper.writeValueAsString(transaction)
        return GetTransactionResponse(
            transaction = objectMapper.readValue(json, Transaction::class.java)
        )
    }

    private fun sync(transactionId: String, context: WorkflowContext) {
        if (context.data[DATA_SYNC] != true) {
            return
        }

        try {
            checkoutAccessApi.syncTransactionStatus(transactionId)
            logger.add("sync_success", true)
        } catch (ex: Exception) {
            logger.add("sync_success", false)
            logger.add("sync_exception", ex::javaClass.name)
            logger.add("sync_error", ex.message)
        }
    }
}
