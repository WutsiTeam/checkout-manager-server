package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.manager.util.SecurityUtil
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.AbstractWorkflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractCheckoutWorkflow<Req, Resp, Ev>(eventStream: EventStream) :
    AbstractWorkflow<Req, Resp, Ev>(eventStream) {
    @Autowired
    protected lateinit var checkoutAccessApi: CheckoutAccessApi

    @Autowired
    protected lateinit var membershipAccess: MembershipAccessApi

    @Autowired
    protected lateinit var marketplaceAccessApi: MarketplaceAccessApi

    protected fun getCurrentAccountId(context: WorkflowContext): Long =
        context.accountId ?: SecurityUtil.getAccountId()

    protected fun getCurrentAccount(context: WorkflowContext): Account {
        val accountId = context.accountId ?: SecurityUtil.getAccountId()
        return membershipAccess.getAccount(accountId).account
    }
}
