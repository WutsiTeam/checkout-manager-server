package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.checkout.manager.event.TransactionEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service

@Service
class ProcessPendingTransactionWorkflow(
    eventStream: EventStream
) : AbstractCheckoutWorkflow<String, Unit, Void?>(eventStream) {
    override fun getEventType(transactionId: String, response: Unit, context: WorkflowContext): String? = null

    override fun toEventPayload(transactionId: String, response: Unit, context: WorkflowContext): Void? = null

    override fun getValidationRules(transactionId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(transactionId: String, context: WorkflowContext) {
        val response = checkoutAccessApi.syncTransactionStatus(transactionId)
        if (response.status == Status.SUCCESSFUL.name) {
            eventStream.enqueue(InternalEventURN.TRANSACTION_SUCCESSFUL.urn, TransactionEventPayload(transactionId))
        }
    }
}
