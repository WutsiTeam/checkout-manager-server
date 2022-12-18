package com.wutsi.checkout.manager.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.checkout.access.dto.Business
import com.wutsi.checkout.access.dto.CreateOrderItemRequest
import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import com.wutsi.enums.ProductStatus
import com.wutsi.error.ErrorURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.marketplace.access.dto.CreateReservationRequest
import com.wutsi.marketplace.access.dto.ReservationItem
import com.wutsi.marketplace.access.dto.SearchProductRequest
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.core.error.exception.ConflictException
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.rule.account.AccountShouldBeActiveRule
import com.wutsi.workflow.rule.account.BusinessShouldBeActive
import feign.FeignException
import org.springframework.stereotype.Service

@Service
class CreateOrderWorkflow(
    private val objectMapper: ObjectMapper,
    private val logger: KVLogger,
    eventStream: EventStream
) : AbstractOrderWorkflow<CreateOrderRequest, CreateOrderResponse>(eventStream) {
    override fun getEventType(
        request: CreateOrderRequest,
        response: CreateOrderResponse,
        context: WorkflowContext
    ): String? = null

    override fun toEventPayload(
        request: CreateOrderRequest,
        response: CreateOrderResponse,
        context: WorkflowContext
    ): OrderEventPayload? = null

    override fun getValidationRules(request: CreateOrderRequest, context: WorkflowContext): RuleSet {
        val business = checkoutAccessApi.getBusiness(request.businessId).business
        val account = membershipAccessApi.getAccount(business.accountId).account
        return RuleSet(
            listOfNotNull(
                AccountShouldBeActiveRule(account),
                BusinessShouldBeActive(business)
            )
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
            orderStatus = response.orderStatus
        )
    }

    private fun createOrder(
        request: CreateOrderRequest,
        business: Business
    ): com.wutsi.checkout.access.dto.CreateOrderResponse {
        val products = marketplaceAccessApi.searchProduct(
            request = SearchProductRequest(
                limit = request.items.size,
                productIds = request.items.map { it.productId },
                status = ProductStatus.PUBLISHED.name
            )
        ).products.associateBy { it.id }

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
                    val prod = products[it.productId]
                    if (prod != null) {
                        CreateOrderItemRequest(
                            productId = it.productId,
                            productType = prod.type,
                            quantity = it.quantity,
                            title = prod.title,
                            pictureUrl = prod.thumbnailUrl,
                            unitPrice = prod.price ?: 0
                        )
                    } else {
                        null
                    }
                }.filterNotNull()
            )
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
                            quantity = it.quantity
                        )
                    }
                )
            ).reservationId
        } catch (ex: FeignException) {
            throw handleAvailabilityException(ex)
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
