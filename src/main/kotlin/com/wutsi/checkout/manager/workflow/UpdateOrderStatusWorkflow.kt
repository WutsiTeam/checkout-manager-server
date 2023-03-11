package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import com.wutsi.checkout.manager.workflow.task.CancelOrderReservationTask
import com.wutsi.enums.OrderStatus
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.marketplace.access.dto.SearchReservationRequest
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import com.wutsi.workflow.rule.account.AccountShouldBeOwnerOfOrder
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class UpdateOrderStatusWorkflow(
    private val workflowEngine: WorkflowEngine,
    private val checkoutAccessApi: CheckoutAccessApi,
    private val membershipAccessApi: MembershipAccessApi,
    private val marketplaceAccessApi: MarketplaceAccessApi,
) : Workflow {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "update-order-status")
    }

    @PostConstruct
    fun init() {
        workflowEngine.register(ID, this)
    }

    override fun execute(context: WorkflowContext) {
        val request = context.input as UpdateOrderStatusRequest
        val order = getOrder(request.orderId)
        if (order.status == request.status) {
            return
        }

        val accountId = context.accountId!!
        val account = getAccount(accountId)
        validate(account, order)

        updateStatus(order, request, context)

        when (request.status.uppercase()) {
            OrderStatus.CANCELLED.name -> cancelReservation(order, context)
            else -> {}
        }
    }

    private fun getOrder(orderId: String): Order =
        checkoutAccessApi.getOrder(orderId).order

    private fun getAccount(accountId: Long): Account =
        membershipAccessApi.getAccount(accountId).account

    fun validate(account: Account, order: Order) =
        RuleSet(
            rules = listOf(
                AccountShouldBeOwnerOfOrder(account, order),
            ),
        ).check()

    fun updateStatus(order: Order, request: UpdateOrderStatusRequest, context: WorkflowContext) =
        checkoutAccessApi.updateOrderStatus(
            id = request.orderId,
            request = com.wutsi.checkout.access.dto.UpdateOrderStatusRequest(
                status = request.status,
                reason = request.reason,
            ),
        )

    fun cancelReservation(order: Order, context: WorkflowContext) {
        marketplaceAccessApi.searchReservation(
            SearchReservationRequest(orderId = order.id),
        ).reservations.forEach {
            workflowEngine.executeAsync(
                CancelOrderReservationTask.ID,
                WorkflowContext(
                    accountId = context.accountId,
                    data = mutableMapOf(
                        CancelOrderReservationTask.CONTEXT_ORDER_ID to order.id,
                    ),
                ),
            )
        }
    }
}
