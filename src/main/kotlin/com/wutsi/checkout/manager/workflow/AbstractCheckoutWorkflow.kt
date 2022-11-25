package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.manager.util.SecurityUtil
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.error.Error
import com.wutsi.platform.core.error.exception.NotFoundException
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.AbstractWorkflow
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.error.ErrorURN
import feign.FeignException
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractCheckoutWorkflow<Req, Resp, Ev>(eventStream: EventStream) :
    AbstractWorkflow<Req, Resp, Ev>(eventStream) {
    @Autowired
    protected lateinit var checkoutAccessApi: CheckoutAccessApi

    @Autowired
    protected lateinit var membershipAccess: MembershipAccessApi

    protected fun getCurrentAccountId(context: WorkflowContext): Long =
        context.accountId ?: SecurityUtil.getAccountId()

    protected fun getCurrentAccount(context: WorkflowContext): Account {
        val accountId = context.accountId ?: SecurityUtil.getAccountId()
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
