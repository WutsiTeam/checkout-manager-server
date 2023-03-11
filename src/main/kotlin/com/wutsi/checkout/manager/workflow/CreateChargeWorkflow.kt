package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.dto.CreateChargeRequest
import com.wutsi.checkout.manager.dto.CreateChargeResponse
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.BusinessShouldBeActive
import com.wutsi.workflow.rule.account.OrderShouldNotBeExpiredRule
import com.wutsi.workflow.rule.account.PaymentMethodShouldBeActive
import feign.FeignException
import org.springframework.stereotype.Service

@Service
class CreateChargeWorkflow(
    eventStream: EventStream,
) : AbstractTransactionWorkflow<CreateChargeRequest, CreateChargeResponse>(eventStream) {
    override fun getValidationRules(request: CreateChargeRequest, context: WorkflowContext): RuleSet {
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val account = membershipAccessApi.getAccount(business.accountId).account
        val paymentMethod = request.paymentMethodToken?.let {
            checkoutAccessApi.getPaymentMethod(it).paymentMethod
        }
        val order = checkoutAccessApi.getOrder(request.orderId).order
        return RuleSet(
            listOfNotNull(
                AccountShouldBeActiveRule(account),
                BusinessShouldBeActive(business),
                paymentMethod?.let { PaymentMethodShouldBeActive(it) },
                OrderShouldNotBeExpiredRule(order),
            ),
        )
    }

    override fun doExecute(request: CreateChargeRequest, context: WorkflowContext): CreateChargeResponse {
        val order = checkoutAccessApi.getOrder(request.orderId).order
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val response = charge(request, business, order)
        logger.add("transaction_id", response.transactionId)
        logger.add("transaction_status", response.status)

        if (response.status == Status.SUCCESSFUL.name) {
            handleSuccessfulTrransaction(response.transactionId, context)
        }
        return CreateChargeResponse(
            transactionId = response.transactionId,
            status = response.status,
        )
    }

    private fun charge(
        request: CreateChargeRequest,
        business: Business,
        order: Order,
    ): com.wutsi.checkout.access.dto.CreateChargeResponse {
        try {
            return checkoutAccessApi.createCharge(
                request = com.wutsi.checkout.access.dto.CreateChargeRequest(
                    email = request.email,
                    orderId = request.orderId,
                    paymentMethodToken = request.paymentMethodToken,
                    paymentMethodType = request.paymentMethodType,
                    paymentProviderId = request.paymentProviderId,
                    paymentMethodOwnerName = request.paymentMethodOwnerName,
                    paymenMethodNumber = request.paymenMethodNumber,
                    businessId = business.id,
                    idempotencyKey = request.idempotencyKey,
                    amount = order.balance,
                    description = request.description,
                ),
            )
        } catch (ex: FeignException) {
            throw handleChargeException(ex)
        }
    }
}
