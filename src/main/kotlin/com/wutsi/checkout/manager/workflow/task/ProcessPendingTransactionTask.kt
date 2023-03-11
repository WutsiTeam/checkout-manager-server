package com.wutsi.checkout.manager.workflow.task

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class ProcessPendingTransactionTask(
    private val workflowEngine: WorkflowEngine,
    private val checkoutAccessApi: CheckoutAccessApi,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "process-pending-transaction")
        const val CONTEXT_TRANSACTION_ID = "transaction-id"
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        val transactionId = context.data[CONTEXT_TRANSACTION_ID] as String
        val response = checkoutAccessApi.syncTransactionStatus(transactionId)
        if (response.status == Status.SUCCESSFUL.name) {
            handleSuccessfulTransaction(transactionId, context)
        }
    }

    private fun handleSuccessfulTransaction(transactionId: String, context: WorkflowContext) =
        workflowEngine.executeAsync(
            HandleSuccessfulTransactionTask.ID,
            context.copy(
                data = mutableMapOf(HandleSuccessfulTransactionTask.CONTEXT_TRANSACTION_ID to transactionId),
            ),
        )
}
