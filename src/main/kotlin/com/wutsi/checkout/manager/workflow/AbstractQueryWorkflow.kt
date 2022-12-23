package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.manager.util.SecurityUtil
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.workflow.Workflow
import com.wutsi.workflow.WorkflowContext
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractQueryWorkflow<Req, Resp> : Workflow<Req, Resp> {
    @Autowired
    protected lateinit var checkoutAccessApi: CheckoutAccessApi

    @Autowired
    protected lateinit var membershipAccessApi: MembershipAccessApi

    @Autowired
    protected lateinit var marketplaceAccessApi: MarketplaceAccessApi

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected fun getCurrentAccountId(context: WorkflowContext): Long =
        context.accountId ?: SecurityUtil.getAccountId()

    protected fun getCurrentAccount(context: WorkflowContext): Account {
        val accountId = context.accountId ?: SecurityUtil.getAccountId()
        return membershipAccessApi.getAccount(accountId).account
    }
}
