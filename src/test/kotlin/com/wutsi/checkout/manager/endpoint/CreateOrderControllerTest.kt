package com.wutsi.checkout.manager.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetBusinessResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.CreateOrderItemRequest
import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import com.wutsi.enums.ChannelType
import com.wutsi.enums.DeviceType
import com.wutsi.enums.OrderStatus
import com.wutsi.event.EventURN
import com.wutsi.event.OrderEventPayload
import com.wutsi.marketplace.access.dto.CreateReservationRequest
import com.wutsi.marketplace.access.dto.CreateReservationResponse
import com.wutsi.marketplace.access.dto.ReservationItem
import com.wutsi.marketplace.access.dto.SearchProductResponse
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.core.error.ErrorResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateOrderControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    val orderId = "1111"
    private val businessAccountId = 33333L
    private val reservationId = 11L
    private val product1 = Fixtures.createProductSummary(1L)
    private val product2 = Fixtures.createProductSummary(2L)
    private val businessAccount =
        Fixtures.createAccount(id = businessAccountId, businessId = BUSINESS_ID, business = true)
    private val business = Fixtures.createBusiness(id = BUSINESS_ID, accountId = businessAccountId)
    private val request = CreateOrderRequest(
        channelType = ChannelType.WEB.name,
        deviceType = DeviceType.MOBILE.name,
        businessId = BUSINESS_ID,
        customerName = "Ray Sponsible",
        customerEmail = "ray.sponsible@gmail.com",
        notes = "This is a message to merchant :-)",
        items = listOf(
            CreateOrderItemRequest(
                productId = product1.id,
                quantity = 1
            ),
            CreateOrderItemRequest(
                productId = product2.id,
                quantity = 2
            )
        )
    )

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(GetAccountResponse(businessAccount)).whenever(membershipAccess).getAccount(businessAccountId)

        doReturn(SearchProductResponse(listOf(product1, product2))).whenever(marketplaceAccessApi).searchProduct(
            any()
        )

        doReturn(CreateReservationResponse(reservationId)).whenever(marketplaceAccessApi).createReservation(any())

        doReturn(GetBusinessResponse(business)).whenever(checkoutAccess).getBusiness(BUSINESS_ID)
    }

    @Test
    fun opened() {
        // OPENED
        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.OPENED.name)).whenever(
            checkoutAccess
        )
            .createOrder(any())

        // WHEN
        val response =
            rest.postForEntity(url(), request, com.wutsi.checkout.manager.dto.CreateOrderResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createOrder(
            request = com.wutsi.checkout.access.dto.CreateOrderRequest(
                deviceType = request.deviceType,
                channelType = request.channelType,
                customerEmail = request.customerEmail,
                notes = request.notes,
                customerName = request.customerName,
                businessId = business.id,
                currency = business.currency,
                items = listOf(
                    com.wutsi.checkout.access.dto.CreateOrderItemRequest(
                        productId = request.items[0].productId,
                        title = product1.title,
                        pictureUrl = product1.thumbnailUrl,
                        quantity = request.items[0].quantity,
                        unitPrice = product1.price ?: 0
                    ),
                    com.wutsi.checkout.access.dto.CreateOrderItemRequest(
                        productId = request.items[1].productId,
                        title = product2.title,
                        pictureUrl = product2.thumbnailUrl,
                        quantity = request.items[1].quantity,
                        unitPrice = product2.price ?: 0
                    )
                )
            )
        )

        verify(marketplaceAccessApi).createReservation(
            request = CreateReservationRequest(
                orderId = orderId,
                items = listOf(
                    ReservationItem(
                        productId = request.items[0].productId,
                        quantity = request.items[0].quantity
                    ),
                    ReservationItem(
                        productId = request.items[1].productId,
                        quantity = request.items[1].quantity
                    )
                )
            )
        )

        verify(eventStream).publish(EventURN.ORDER_OPENED.urn, OrderEventPayload(orderId = orderId))
    }

    @Test
    fun pending() {
        // GIVEN
        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.UNKNOWN.name)).whenever(
            checkoutAccess
        )
            .createOrder(any())

        // WHEN
        val response = rest.postForEntity(url(), request, CreateOrderResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createOrder(any())
        verify(marketplaceAccessApi).createReservation(any())

        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun `availability error`() {
        // GIVEN
        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.UNKNOWN.name)).whenever(
            checkoutAccess
        )
            .createOrder(any())

        // GIVEN
        val cause = createFeignNotFoundException(com.wutsi.marketplace.access.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn)
        doThrow(cause).whenever(marketplaceAccessApi).createReservation(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CreateOrderResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(checkoutAccess).createOrder(any())
        verify(marketplaceAccessApi, never()).createReservation(any())

        verify(eventStream, never()).publish(any(), any())

        val response = ObjectMapper().readValue(ex.responseBodyAsString, ErrorResponse::class.java)
        assertEquals(com.wutsi.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn, response.error.code)
    }

    private fun url(): String = "http://localhost:$port/v1/orders"
}
