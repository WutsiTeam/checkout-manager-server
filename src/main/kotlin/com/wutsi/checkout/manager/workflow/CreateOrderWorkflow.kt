package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.CreateOrderDiscountRequest
import com.wutsi.checkout.access.dto.CreateOrderItemRequest
import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import com.wutsi.error.ErrorURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.marketplace.access.dto.CreateReservationRequest
import com.wutsi.marketplace.access.dto.ReservationItem
import com.wutsi.marketplace.access.dto.SearchDiscountRequest
import com.wutsi.marketplace.access.dto.SearchOfferRequest
import com.wutsi.platform.core.error.Error
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.core.error.exception.ConflictException
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.BusinessShouldBeActive
import feign.FeignException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime

@Service
class CreateOrderWorkflow(
    private val objectMapper: ObjectMapper,
    private val logger: KVLogger,
    private val clock: Clock,

    @Value("\${wutsi.application.order.ttl-minutes}") private val ttlMinutes: Long,

    eventStream: EventStream,
) : AbstractOrderWorkflow<CreateOrderRequest, CreateOrderResponse>(eventStream) {
    override fun getEventType(
        request: CreateOrderRequest,
        response: CreateOrderResponse,
        context: WorkflowContext,
    ): String? = null

    override fun toEventPayload(
        request: CreateOrderRequest,
        response: CreateOrderResponse,
        context: WorkflowContext,
    ): OrderEventPayload? = null

    override fun getValidationRules(request: CreateOrderRequest, context: WorkflowContext): RuleSet {
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val account = membershipAccessApi.getAccount(business.accountId).account
        return RuleSet(
            listOfNotNull(
                AccountShouldBeActiveRule(account),
                BusinessShouldBeActive(business),
            ),
        )
    }

    override fun doExecute(request: CreateOrderRequest, context: WorkflowContext): CreateOrderResponse {
        val business = checkoutAccessApi.getBusiness(request.businessId).business

        // Order
        val response = createOrder(request, business)
        logger.add("order_id", response.orderId)
        logger.add("order_status", response.orderStatus)

        // Reserve products
        val reservationId = createReservation(request, response.orderId)
        logger.add("reservation_id", reservationId)

        return CreateOrderResponse(
            orderId = response.orderId,
            orderStatus = response.orderStatus,
        )
    }

    private fun createOrder(
        request: CreateOrderRequest,
        business: Business,
    ): com.wutsi.checkout.access.dto.CreateOrderResponse {
        // Offers
        val offers = marketplaceAccessApi.searchOffer(
            request = SearchOfferRequest(
                limit = request.items.size,
                productIds = request.items.map { it.productId },
            ),
        ).offers.associateBy { it.product.id }

        // Discounts
        val discountIds = offers.mapNotNull { it.value.price.discountId }.toSet()
        val discounts = if (discountIds.isEmpty()) {
            emptyMap()
        } else {
            marketplaceAccessApi.searchDiscount(
                request = SearchDiscountRequest(
                    discountIds = discountIds.toList(),
                    limit = discountIds.size,
                ),
            ).discounts.associateBy { it.id }
        }

        return checkoutAccessApi.createOrder(
            request = com.wutsi.checkout.access.dto.CreateOrderRequest(
                businessId = business.id,
                notes = request.notes,
                currency = business.currency,
                customerEmail = request.customerEmail,
                customerName = request.customerName,
                deviceType = request.deviceType,
                channelType = request.channelType,
                items = request.items.map {
                    val offer = offers[it.productId]
                    if (offer == null) {
                        throw ConflictException(
                            error = Error(
                                code = ErrorURN.PRODUCT_NOT_AVAILABLE.urn,
                                data = mapOf(
                                    "product-id" to it.productId,
                                ),
                            ),
                        )
                    } else {
                        val discount = offer.price.discountId?.let { discounts[it] }
                        val product = offer.product
                        CreateOrderItemRequest(
                            productId = it.productId,
                            productType = product.type,
                            quantity = it.quantity,
                            title = product.title,
                            pictureUrl = product.thumbnailUrl,
                            unitPrice = product.price ?: 0,
                            discounts = if (discount != null) {
                                listOf(
                                    CreateOrderDiscountRequest(
                                        discountId = discount.id,
                                        name = discount.name,
                                        type = discount.type,
                                        amount = offer.price.savings,
                                    ),
                                )
                            } else {
                                emptyList()
                            },
                        )
                    }
                },
                expires = OffsetDateTime.now(clock).plusMinutes(ttlMinutes),
            ),
        )
    }

    private fun createReservation(request: CreateOrderRequest, orderId: String): Long {
        try {
            return marketplaceAccessApi.createReservation(
                request = CreateReservationRequest(
                    orderId = orderId,
                    items = request.items.map {
                        ReservationItem(
                            productId = it.productId,
                            quantity = it.quantity,
                        )
                    },
                ),
            ).reservationId
        } catch (ex: FeignException) {
            throw handleAvailabilityException(ex)
        }
    }

    private fun handleAvailabilityException(ex: FeignException): Throwable {
        val response = objectMapper.readValue(ex.contentUTF8(), ErrorResponse::class.java)
        if (response.error.code == com.wutsi.marketplace.access.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn) {
            return ConflictException(
                error = response.error.copy(code = ErrorURN.PRODUCT_NOT_AVAILABLE.urn),
            )
        } else {
            return ex
        }
    }
}
