package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.UpdatePaymentMethodStatusRequest
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.event.PaymentMethodEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.AccountShouldBeOwnerOfPaymentMethodRule
import org.springframework.stereotype.Service

@Service
class DeactivatePaymentMethodWorkflow(
    eventStream: EventStream
) : AbstractPaymentMethodWorkflow<String, Unit>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(
        request: String,
        response: Unit,
        context: WorkflowContext
    ): PaymentMethodEventPayload? = null

    override fun getValidationRules(token: String, context: WorkflowContext): RuleSet {
        val account = getCurrentAccount(context)
        val paymentMethod = checkoutAccessApi.getPaymentMethod(token).paymentMethod
        return RuleSet(
            listOf(
                AccountShouldBeActiveRule(account),
                AccountShouldBeOwnerOfPaymentMethodRule(account, paymentMethod)
            )
        )
    }

    override fun doExecute(token: String, context: WorkflowContext) {
        checkoutAccessApi.updatePaymentMethodStatus(
            token = token,
            request = UpdatePaymentMethodStatusRequest(
                status = PaymentMethodStatus.INACTIVE.name
            )
        )
    }
}
