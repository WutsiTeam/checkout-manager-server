package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.dto.Transaction
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetTransactionWorkflow(
    private val objectMapper: ObjectMapper,
    eventStream: EventStream
) : AbstractCheckoutWorkflow<String, GetTransactionResponse, Void>(eventStream) {
    override fun getEventType(
        transactionId: String,
        response: GetTransactionResponse,
        context: WorkflowContext
    ): String? = null

    override fun toEventPayload(
        transactionId: String,
        response: GetTransactionResponse,
        context: WorkflowContext
    ): Void? = null

    override fun getValidationRules(transactionId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(transactionId: String, context: WorkflowContext): GetTransactionResponse {
        val transaction = checkoutAccessApi.getTransaction(transactionId).transaction
        /*
         * Because the order data structure is the same as the one from access layer, let use json serialization for copying the data
         */
        val json = objectMapper.writeValueAsString(transaction)
        return GetTransactionResponse(
            transaction = objectMapper.readValue(json, Transaction::class.java)
        )
    }
}
