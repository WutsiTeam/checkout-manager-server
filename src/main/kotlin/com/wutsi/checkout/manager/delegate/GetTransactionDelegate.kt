package com.wutsi.checkout.manager.`delegate`

import com.wutsi.checkout.manager.dto.GetTransactionResponse
import com.wutsi.checkout.manager.workflow.GetTransactionWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class GetTransactionDelegate(private val workflow: GetTransactionWorkflow) {
    public fun invoke(id: String): GetTransactionResponse =
        workflow.execute(id, WorkflowContext())
}
