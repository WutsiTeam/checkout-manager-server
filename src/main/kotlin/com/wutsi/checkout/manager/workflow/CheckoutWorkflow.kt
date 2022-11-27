package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.CreateChargeRequest
import com.wutsi.checkout.access.dto.CreateChargeResponse
import com.wutsi.checkout.access.dto.CreateOrderItemRequest
import com.wutsi.checkout.access.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CheckoutRequest
import com.wutsi.checkout.manager.dto.CheckoutResponse
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.enums.OfferType
import com.wutsi.error.ErrorURN
import com.wutsi.event.CheckoutEventPayload
import com.wutsi.event.EventURN
import com.wutsi.marketplace.access.dto.CheckProductAvailabilityRequest
import com.wutsi.marketplace.access.dto.CreateReservationRequest
import com.wutsi.marketplace.access.dto.ReservationItem
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.core.error.exception.ConflictException
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.payment.core.Status
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.BusinessShouldBeActive
import com.wutsi.workflow.rule.account.PaymentMethodShouldBeActive
import feign.FeignException
import org.springframework.stereotype.Service

@Service
class CheckoutWorkflow(
    private val objectMapper: ObjectMapper,
    private val logger: KVLogger,
    eventStream: EventStream
) : AbstractCheckoutWorkflow<CheckoutRequest, CheckoutResponse, CheckoutEventPayload>(eventStream) {
    override fun getEventType() = EventURN.BUSINESS_CREATED.urn

    override fun toEventPayload(
        request: CheckoutRequest,
        response: CheckoutResponse,
        context: WorkflowContext
    ): CheckoutEventPayload? =
        null

    override fun getValidationRules(request: CheckoutRequest, context: WorkflowContext): RuleSet {
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val account = membershipAccess.getAccount(business.accountId).account
        val paymentMethod = request.paymentMethodToken?.let {
            checkoutAccessApi.getPaymentMethod(it).paymentMethod
        }
        return RuleSet(
            listOfNotNull(
                AccountShouldBeActiveRule(account),
                BusinessShouldBeActive(business),
                paymentMethod?.let { PaymentMethodShouldBeActive(it) }
            )
        )
    }

    override fun doExecute(request: CheckoutRequest, context: WorkflowContext): CheckoutResponse {
        val business = checkoutAccessApi.getBusiness(request.businessId).business

        // Check availability
        checkAvailability(request)

        // Order
        val orderId = createOrder(request, business)
        logger.add("order_id", orderId)

        // Reserve products
        val reservationId = createReservation(request, orderId)
        logger.add("reservation_id", reservationId)

        // Charge customer
        val charge = createCharge(request, business, orderId, reservationId)
        logger.add("transaction_id", charge.transactionId)
        logger.add("transaction_status", charge.status)
        if (charge.status == Status.SUCCESSFUL.name) {
            eventStream.enqueue(
                InternalEventURN.CHARGE_SUCESSFULL.urn,
                CheckoutEventPayload(
                    orderId = orderId,
                    reservationId = reservationId
                )
            )
        }

        return CheckoutResponse(
            orderId = orderId,
            transactionId = charge.transactionId,
            transactionStatus = charge.status
        )
    }

    private fun checkAvailability(request: CheckoutRequest) {
        try {
            marketplaceAccessApi.checkProductAvailability(
                request = CheckProductAvailabilityRequest(
                    items = listOf(
                        ReservationItem(
                            productId = request.productId,
                            quantity = request.quantity
                        )
                    )
                )
            )
        } catch (ex: FeignException) {
            throw handleAvailabilityException(ex)
        }
    }

    private fun createOrder(request: CheckoutRequest, business: Business): String {
        val product = marketplaceAccessApi.getProduct(request.productId).product
        return checkoutAccessApi.createOrder(
            request = CreateOrderRequest(
                businessId = business.id,
                notes = request.notes,
                currency = business.currency,
                customerEmail = request.customerEmail,
                customerName = request.customerName,
                customerId = request.customerId,
                deviceType = request.deviceType,
                channelType = request.channelType,
                items = listOf(
                    CreateOrderItemRequest(
                        offerId = request.productId,
                        offerType = OfferType.PRODUCT.name,
                        title = product.title,
                        pictureUrl = product.thumbnail?.url,
                        unitPrice = product.price ?: 0,
                        quantity = request.quantity
                    )
                )
            )
        ).orderId
    }

    private fun createReservation(request: CheckoutRequest, orderId: String): Long {
        try {
            return marketplaceAccessApi.createReservation(
                request = CreateReservationRequest(
                    orderId = orderId,
                    items = listOf(
                        ReservationItem(
                            productId = request.productId,
                            quantity = request.quantity
                        )
                    )
                )
            ).reservationId
        } catch (ex: FeignException) {
            throw handleAvailabilityException(ex)
        }
    }

    private fun createCharge(
        request: CheckoutRequest,
        business: Business,
        orderId: String,
        reservationId: Long
    ): CreateChargeResponse {
        val order = checkoutAccessApi.getOrder(orderId).order

        // Free?
        if (order.totalPrice == null || order.totalPrice == 0L) {
            return CreateChargeResponse(
                transactionId = "",
                status = Status.SUCCESSFUL.name
            )
        }

        // Not Free
        try {
            return checkoutAccessApi.createCharge(
                request = CreateChargeRequest(
                    email = request.customerEmail,
                    orderId = order.id,
                    paymentMethodToken = request.paymentMethodToken,
                    paymentMethodType = request.paymentMethodType,
                    paymentProviderId = request.paymentProviderId,
                    paymentMethodOwnerName = request.customerName,
                    paymenMethodNumber = request.paymenMethodNumber,
                    businessId = business.id,
                    amount = order.totalPrice,
                    idempotencyKey = request.idempotencyKey
                )
            )
        } catch (ex: FeignException) {
            eventStream.enqueue(
                InternalEventURN.CHARGE_FAILED.urn,
                CheckoutEventPayload(
                    orderId = orderId,
                    reservationId = reservationId
                )
            )
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

    private fun handleAvailabilityException(ex: FeignException): Throwable {
        val response = objectMapper.readValue(ex.contentUTF8(), ErrorResponse::class.java)
        if (response.error.code == com.wutsi.marketplace.access.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn) {
            return ConflictException(
                error = response.error.copy(code = ErrorURN.PRODUCT_NOT_AVAILABLE.urn)
            )
        } else {
            return ex
        }
    }
}
