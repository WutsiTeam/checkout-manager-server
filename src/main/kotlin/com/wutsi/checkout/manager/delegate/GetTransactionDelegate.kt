package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.workflow.GetTransactionWorkflow
import com.wutsi.checkout.manager.workflow.task.ProcessPendingTransactionTask
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.WorkflowEngine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
public class GetTransactionDelegate(
    private val workflow: GetTransactionWorkflow,
    private val workflowEngine: WorkflowEngine,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetTransactionDelegate::class.java)
    }

    public fun invoke(id: String, sync: Boolean?): GetTransactionResponse {
        val context = WorkflowContext(
            data = mutableMapOf(
                ProcessPendingTransactionTask.CONTEXT_TRANSACTION_ID to id,
            ),
        )

        if (sync == true) {
            try {
                workflowEngine.execute(
                    ProcessPendingTransactionTask.ID,
                    context,
                )
            } catch (ex: Exception) {
                LOGGER.warn("Unable to sync the transaction", ex)
            }
        }
        return workflow.execute(id, context)
    }
}
