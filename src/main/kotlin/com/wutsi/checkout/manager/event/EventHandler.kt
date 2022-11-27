package com.wutsi.checkout.manager.event

import com.wutsi.event.EventURN
import com.wutsi.platform.core.stream.Event
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(
    private val membership: MembershipEventHandler,
    private val transaction: TransactionEventHandler
) {
    @EventListener
    fun handleEvent(event: Event) {
        when (event.type) {
            EventURN.MEMBER_REGISTERED.urn -> membership.onMemberRegistered(event)
            EventURN.BUSINESS_ACCOUNT_ENABLED.urn -> membership.onBusinessAccountEnabled(event)
            EventURN.BUSINESS_ACCOUNT_DISABLED.urn -> membership.onBusinessAccountDisabled(event)
            InternalEventURN.CHARGE_SUCESSFULL.urn -> transaction.onChargeSuccessful(event)
            InternalEventURN.TRANSACTION_PENDING.urn -> transaction.onTransactionPending(event)
            else -> {}
        }
    }
}
