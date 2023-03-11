package com.wutsi.checkout.manager.delegate

import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.manager.util.SecurityUtil
import com.wutsi.checkout.manager.workflow.UpdateOrderStatusWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
public class UpdateOrderStatusDelegate(private val workflow: UpdateOrderStatusWorkflow) {
    public fun invoke(request: UpdateOrderStatusRequest) {
        workflow.execute(
            WorkflowContext(
                input = request,
                accountId = SecurityUtil.getAccountId(),
            ),
        )
    }
}
