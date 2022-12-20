package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.SearchTransactionRequest
import com.wutsi.checkout.manager.dto.SearchTransactionResponse
import com.wutsi.checkout.manager.dto.TransactionSummary
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class SearchTransactionWorkflow(
    eventStream: EventStream,
) : AbstractTransactionWorkflow<SearchTransactionRequest, SearchTransactionResponse>(eventStream) {
    override fun getValidationRules(request: SearchTransactionRequest, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(request: SearchTransactionRequest, context: WorkflowContext): SearchTransactionResponse {
        val transactions = checkoutAccessApi.searchTransaction(
            request = com.wutsi.checkout.access.dto.SearchTransactionRequest(
                customerId = request.customerId,
                businessId = request.businessId,
                type = request.type,
                orderId = request.orderId,
                status = request.status,
                limit = request.limit,
                offset = request.offset,
            ),
        ).transactions
        return SearchTransactionResponse(
            transactions = transactions.map {
                objectMapper.readValue(
                    objectMapper.writeValueAsString(it),
                    TransactionSummary::class.java,
                )
            },
        )
    }
}
