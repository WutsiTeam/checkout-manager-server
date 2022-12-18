package com.wutsi.checkout.manager.event

import com.wutsi.event.EventURN
import com.wutsi.platform.core.stream.Event
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(
    private val membership: MembershipEventHandler,
    private val transaction: TransactionEventHandler,
    private val order: OrderEventHandler
) {
    @EventListener
    fun handleEvent(event: Event) {
        when (event.type) {
            EventURN.MEMBER_REGISTERED.urn -> membership.onMemberRegistered(event)
            EventURN.MEMBER_DELETED.urn -> membership.onMemberDeleted(event)
            EventURN.BUSINESS_ACTIVATED.urn -> membership.onBusinessActivated(event)
            EventURN.BUSINESS_DEACTIVATED.urn -> membership.onBusinesstDeactivated(event)
            InternalEventURN.TRANSACTION_SUCCESSFUL.urn -> transaction.onTransactionSuccessful(event)
            InternalEventURN.ORDER_TO_CUSTOMER_SUBMITTED.urn -> order.onSendToCustomer(event)
            InternalEventURN.ORDER_TO_MERCHANT_SUBMITTED.urn -> order.onSendToMerchant(event)
            InternalEventURN.ORDER_FULLFILLED.urn -> order.onOrderFulfilled(event)
            else -> {}
        }
    }
}
