package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.CreateBusinessRequest
import com.wutsi.checkout.manager.util.SecurityUtil
import com.wutsi.checkout.manager.workflow.CreateBusinessWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class CreateBusinessDelegate(private val workflow: CreateBusinessWorkflow) {
    public fun invoke(request: CreateBusinessRequest) {
        val context = WorkflowContext(
            accountId = SecurityUtil.getAccountId(),
            input = request,
        )
        workflow.execute(context)
    }
}
