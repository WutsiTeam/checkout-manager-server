package com.wutsi.checkout.manager.event

import com.wutsi.membership.manager.event.EventURN
import com.wutsi.platform.core.stream.Event
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(
    private val membership: MembershipEventHandler
) {
    @EventListener
    fun handleEvent(event: Event) {
        when (event.type) {
            EventURN.MEMBER_REGISTERED.urn -> membership.onMemberRegistered(event)
            EventURN.BUSINESS_ACCOUNT_ENABLED.urn -> membership.onBusinessAccountEnabled(event)
            else -> {}
        }
    }
}
