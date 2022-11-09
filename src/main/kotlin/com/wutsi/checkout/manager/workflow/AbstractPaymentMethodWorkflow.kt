package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.manager.event.PaymentMethodEventPayload
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.membership.manager.util.SecurityUtil
import com.wutsi.platform.core.error.Error
import com.wutsi.platform.core.error.exception.NotFoundException
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.AbstractWorkflow
import com.wutsi.workflow.error.ErrorURN
import feign.FeignException
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractPaymentMethodWorkflow(eventStream: EventStream) :
    AbstractWorkflow<PaymentMethodEventPayload>(eventStream) {
    @Autowired
    protected lateinit var checkoutAccess: CheckoutAccessApi

    @Autowired
    protected lateinit var membershipAccess: MembershipAccessApi

    protected fun getCurrentAccount(): Account {
        val accountId = SecurityUtil.getAccountId()
        try {
            return membershipAccess.getAccount(accountId).account
        } catch (ex: FeignException.NotFound) {
            throw NotFoundException(
                error = Error(
                    code = ErrorURN.MEMBER_NOT_FOUND.urn,
                    data = mapOf(
                        "account-id" to accountId
                    )
                )
            )
        }
    }
}
