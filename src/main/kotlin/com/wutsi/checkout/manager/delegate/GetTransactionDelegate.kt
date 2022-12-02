package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.workflow.GetTransactionWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class GetTransactionDelegate(private val workflow: GetTransactionWorkflow) {
    public fun invoke(id: String, sync: Boolean?): GetTransactionResponse =
        workflow.execute(
            id,
            WorkflowContext(
                data = sync?.let { mutableMapOf(GetTransactionWorkflow.DATA_SYNC to it) }
                    ?: mutableMapOf()
            )
        )
}
