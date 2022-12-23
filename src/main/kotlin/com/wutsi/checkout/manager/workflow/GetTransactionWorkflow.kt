package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.dto.Transaction
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class GetTransactionWorkflow : AbstractQueryWorkflow<String, GetTransactionResponse>() {
    override fun execute(transactionId: String, context: WorkflowContext): GetTransactionResponse {
        val transaction = checkoutAccessApi.getTransaction(transactionId).transaction
        return GetTransactionResponse(
            transaction = objectMapper.readValue(
                objectMapper.writeValueAsString(transaction),
                Transaction::class.java,
            ),
        )
    }
}
