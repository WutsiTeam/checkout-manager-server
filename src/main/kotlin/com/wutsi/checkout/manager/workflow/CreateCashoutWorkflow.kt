package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.manager.dto.CreateCashoutRequest
import com.wutsi.checkout.manager.dto.CreateCashoutResponse
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.AccountShouldBeBusinessRule
import com.wutsi.workflow.rule.account.BusinessShouldBeActive
import com.wutsi.workflow.rule.account.PaymentMethodShouldBeActive
import feign.FeignException
import org.springframework.stereotype.Service

@Service
class CreateCashoutWorkflow(
    eventStream: EventStream,
) : AbstractTransactionWorkflow<CreateCashoutRequest, CreateCashoutResponse>(eventStream) {
    override fun getValidationRules(request: CreateCashoutRequest, context: WorkflowContext): RuleSet {
        val account = getCurrentAccount(context)
        val business = account.businessId?.let {
            checkoutAccessApi.getBusiness(it).business
        }
        val paymentMethod = checkoutAccessApi.getPaymentMethod(request.paymentMethodToken).paymentMethod

        return RuleSet(
            listOfNotNull(
                AccountShouldBeActiveRule(account),
                AccountShouldBeBusinessRule(account),
                business?.let { BusinessShouldBeActive(it) },
                PaymentMethodShouldBeActive(paymentMethod),
            ),
        )
    }

    override fun doExecute(request: CreateCashoutRequest, context: WorkflowContext): CreateCashoutResponse {
        val account = getCurrentAccount(context)
        val response = cashout(request, account)
        logger.add("transaction_id", response.transactionId)
        logger.add("transaction_status", response.status)

        if (response.status == Status.SUCCESSFUL.name) {
            handleSuccessfulTrransaction(response.transactionId, context)
        }
        return CreateCashoutResponse(
            transactionId = response.transactionId,
            status = response.status,
        )
    }

    private fun cashout(
        request: CreateCashoutRequest,
        account: Account,
    ): com.wutsi.checkout.access.dto.CreateCashoutResponse {
        try {
            return checkoutAccessApi.createCashout(
                request = com.wutsi.checkout.access.dto.CreateCashoutRequest(
                    email = account.email ?: "",
                    businessId = account.businessId!!,
                    paymentMethodToken = request.paymentMethodToken,
                    idempotencyKey = request.idempotencyKey,
                    amount = request.amount,
                    description = request.description,
                ),
            )
        } catch (ex: FeignException) {
            throw handleChargeException(ex)
        }
    }
}
