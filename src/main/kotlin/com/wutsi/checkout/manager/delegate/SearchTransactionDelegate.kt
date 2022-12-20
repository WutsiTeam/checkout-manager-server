package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.SearchTransactionRequest
import com.wutsi.checkout.manager.dto.SearchTransactionResponse
import com.wutsi.checkout.manager.workflow.SearchTransactionWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class SearchTransactionDelegate(private val workflow: SearchTransactionWorkflow) {
    public fun invoke(request: SearchTransactionRequest): SearchTransactionResponse =
        workflow.execute(request, WorkflowContext())
}
