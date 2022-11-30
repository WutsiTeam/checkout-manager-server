package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.dto.CreateChargeRequest
import com.wutsi.checkout.manager.dto.CreateChargeResponse
import com.wutsi.error.ErrorURN
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.core.error.exception.ConflictException
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
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
    private val objectMapper: ObjectMapper,
    private val logger: KVLogger,
    eventStream: EventStream
) : AbstractCheckoutWorkflow<CreateChargeRequest, CreateChargeResponse, Void>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(
        request: CreateChargeRequest,
        response: CreateChargeResponse,
        context: WorkflowContext
    ): Void? = null

    override fun getValidationRules(request: CreateChargeRequest, context: WorkflowContext): RuleSet {
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val account = membershipAccess.getAccount(business.accountId).account
        val paymentMethod = request.paymentMethodToken?.let {
            checkoutAccessApi.getPaymentMethod(it).paymentMethod
        }
        val order = checkoutAccessApi.getOrder(request.orderId).order
        return RuleSet(
            listOfNotNull(
                AccountShouldBeActiveRule(account),
                BusinessShouldBeActive(business),
                paymentMethod?.let { PaymentMethodShouldBeActive(it) },
                OrderShouldNotBeExpiredRule(order)
            )
        )
    }

    override fun doExecute(request: CreateChargeRequest, context: WorkflowContext): CreateChargeResponse {
        val order = checkoutAccessApi.getOrder(request.orderId).order
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val charge = createCharge(request, business, order)
        logger.add("transaction_id", charge.transactionId)
        logger.add("transaction_status", charge.status)

        return CreateChargeResponse(
            transactionId = charge.transactionId,
            status = charge.status
        )
    }

    private fun createCharge(
        request: CreateChargeRequest,
        business: Business,
        order: Order
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
                    description = request.description
                )
            )
        } catch (ex: FeignException) {
            throw handleChargeException(ex)
        }
    }

    private fun handleChargeException(ex: FeignException): Throwable {
        val response = objectMapper.readValue(ex.contentUTF8(), ErrorResponse::class.java)
        if (response.error.code == com.wutsi.checkout.access.error.ErrorURN.TRANSACTION_FAILED.urn) {
            return ConflictException(
                error = response.error.copy(code = ErrorURN.TRANSACTION_FAILED.urn)
            )
        } else {
            return ex
        }
    }
}
