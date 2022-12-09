package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import com.wutsi.enums.OrderStatus
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.AccountShouldBeBusinessRule
import com.wutsi.workflow.rule.account.AccountShouldBeOwnerOfOrder
import org.springframework.stereotype.Service

@Service
class UpdateOrderStatusWorkflow(
    eventStream: EventStream
) : AbstractOrderWorkflow<UpdateOrderStatusRequest, Unit>(eventStream) {
    override fun getEventType(request: UpdateOrderStatusRequest, response: Unit, context: WorkflowContext): String? =
        when (request.status) {
            OrderStatus.IN_PROGRESS.name -> EventURN.ORDER_STARTED.urn
            OrderStatus.COMPLETED.name -> EventURN.ORDER_COMPLETED.urn
            OrderStatus.CANCELLED.name -> EventURN.ORDER_CANCELLED.urn
            else -> null
        }

    override fun toEventPayload(request: UpdateOrderStatusRequest, response: Unit, context: WorkflowContext) =
        OrderEventPayload(
            orderId = request.orderId
        )

    override fun getValidationRules(request: UpdateOrderStatusRequest, context: WorkflowContext): RuleSet {
        val account = getCurrentAccount(context)
        val order = checkoutAccessApi.getOrder(request.orderId).order
        return RuleSet(
            rules = listOf(
                AccountShouldBeActiveRule(account),
                AccountShouldBeBusinessRule(account),
                AccountShouldBeOwnerOfOrder(account, order)
            )
        )
    }

    override fun doExecute(request: UpdateOrderStatusRequest, context: WorkflowContext) {
        checkoutAccessApi.updateOrderStatus(
            id = request.orderId,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason
            )
        )
    }
}
