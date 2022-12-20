package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.workflow.GetTransactionWorkflow
import com.wutsi.checkout.manager.workflow.ProcessPendingTransactionWorkflow
import com.wutsi.workflow.WorkflowContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
public class GetTransactionDelegate(
    private val workflow: GetTransactionWorkflow,
    private val pendingWorkflow: ProcessPendingTransactionWorkflow,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetTransactionDelegate::class.java)
    }

    public fun invoke(id: String, sync: Boolean?): GetTransactionResponse {
        val context = WorkflowContext()

        if (sync == true) {
            try {
                pendingWorkflow.execute(id, context)
            } catch (ex: Exception) {
                LOGGER.warn("Unable to sync the transaction", ex)
            }
        }
        return workflow.execute(id, context)
    }
}
