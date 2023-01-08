package com.wutsi.checkout.manager.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.CreateOrderDiscountRequest
import com.wutsi.checkout.access.dto.GetBusinessResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.CreateOrderItemRequest
import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import com.wutsi.enums.ChannelType
import com.wutsi.enums.DeviceType
import com.wutsi.enums.OrderStatus
import com.wutsi.enums.ProductType
import com.wutsi.marketplace.access.dto.CreateReservationRequest
import com.wutsi.marketplace.access.dto.CreateReservationResponse
import com.wutsi.marketplace.access.dto.ReservationItem
import com.wutsi.marketplace.access.dto.SearchDiscountResponse
import com.wutsi.marketplace.access.dto.SearchOfferResponse
import com.wutsi.marketplace.access.dto.SearchProductResponse
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.core.error.ErrorResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateOrderControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    val orderId = "1111"
    private val businessAccountId = 33333L
    private val reservationId = 11L
    private val product1 = Fixtures.createProductSummary(1L, type = ProductType.PHYSICAL_PRODUCT)
    private val product2 = Fixtures.createProductSummary(2L, type = ProductType.EVENT)
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
                quantity = 1,
            ),
            CreateOrderItemRequest(
                productId = product2.id,
                quantity = 2,
            ),
        ),
    )

    private val now = Instant.ofEpochMilli(10000)
    private val utc = ZoneId.of("UTC")

    @MockBean
    private lateinit var clock: Clock

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(now).whenever(clock).instant()
        doReturn(utc).whenever(clock).zone

        doReturn(GetAccountResponse(businessAccount)).whenever(membershipAccess).getAccount(businessAccountId)

        doReturn(SearchProductResponse(listOf(product1, product2))).whenever(marketplaceAccessApi).searchProduct(
            any(),
        )

        doReturn(CreateReservationResponse(reservationId)).whenever(marketplaceAccessApi).createReservation(any())

        doReturn(GetBusinessResponse(business)).whenever(checkoutAccess).getBusiness(BUSINESS_ID)

        val offers = listOf(
            Fixtures.createOfferSummary(product1, Fixtures.createOfferPrice(product1.id)),
            Fixtures.createOfferSummary(product2, Fixtures.createOfferPrice(product2.id)),
        )
        doReturn(SearchOfferResponse(offers)).whenever(marketplaceAccessApi).searchOffer(any())
    }

    @Test
    fun opened() {
        // GIVEN
        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.OPENED.name)).whenever(
            checkoutAccess,
        )
            .createOrder(any())

        val offers = listOf(
            Fixtures.createOfferSummary(
                product1,
                Fixtures.createOfferPrice(product1.id, discountId = 11, savings = 100),
            ),
            Fixtures.createOfferSummary(product2, Fixtures.createOfferPrice(product2.id)),
        )
        doReturn(SearchOfferResponse(offers)).whenever(marketplaceAccessApi).searchOffer(any())

        val discount = Fixtures.createDiscountSummary(id = 11)
        doReturn(SearchDiscountResponse(listOf(discount))).whenever(marketplaceAccessApi).searchDiscount(any())

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
                expires = OffsetDateTime.now(clock).plusMinutes(10),
                items = listOf(
                    com.wutsi.checkout.access.dto.CreateOrderItemRequest(
                        productId = request.items[0].productId,
                        productType = product1.type,
                        title = product1.title,
                        pictureUrl = product1.thumbnailUrl,
                        quantity = request.items[0].quantity,
                        unitPrice = product1.price ?: 0,
                        discounts = listOf(
                            CreateOrderDiscountRequest(
                                discountId = discount.id,
                                name = discount.name,
                                type = discount.type,
                                amount = offers[0].price.savings,
                            ),
                        ),
                    ),
                    com.wutsi.checkout.access.dto.CreateOrderItemRequest(
                        productId = request.items[1].productId,
                        productType = product2.type,
                        title = product2.title,
                        pictureUrl = product2.thumbnailUrl,
                        quantity = request.items[1].quantity,
                        unitPrice = product2.price ?: 0,
                        discounts = emptyList(),
                    ),
                ),
            ),
        )

        verify(marketplaceAccessApi).createReservation(
            request = CreateReservationRequest(
                orderId = orderId,
                items = listOf(
                    ReservationItem(
                        productId = request.items[0].productId,
                        quantity = request.items[0].quantity,
                    ),
                    ReservationItem(
                        productId = request.items[1].productId,
                        quantity = request.items[1].quantity,
                    ),
                ),
            ),
        )

        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun orderWithDiscounts() {
        // GIVEN
        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.OPENED.name)).whenever(
            checkoutAccess,
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
                expires = OffsetDateTime.now(clock).plusMinutes(10),
                items = listOf(
                    com.wutsi.checkout.access.dto.CreateOrderItemRequest(
                        productId = request.items[0].productId,
                        productType = product1.type,
                        title = product1.title,
                        pictureUrl = product1.thumbnailUrl,
                        quantity = request.items[0].quantity,
                        unitPrice = product1.price ?: 0,
                        discounts = emptyList(),
                    ),
                    com.wutsi.checkout.access.dto.CreateOrderItemRequest(
                        productId = request.items[1].productId,
                        productType = product2.type,
                        title = product2.title,
                        pictureUrl = product2.thumbnailUrl,
                        quantity = request.items[1].quantity,
                        unitPrice = product2.price ?: 0,
                        discounts = emptyList(),
                    ),
                ),
            ),
        )

        verify(marketplaceAccessApi).createReservation(
            request = CreateReservationRequest(
                orderId = orderId,
                items = listOf(
                    ReservationItem(
                        productId = request.items[0].productId,
                        quantity = request.items[0].quantity,
                    ),
                    ReservationItem(
                        productId = request.items[1].productId,
                        quantity = request.items[1].quantity,
                    ),
                ),
            ),
        )

        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun pending() {
        // GIVEN
        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.UNKNOWN.name)).whenever(
            checkoutAccess,
        )
            .createOrder(any())

        doReturn(com.wutsi.checkout.access.dto.CreateOrderResponse(orderId, OrderStatus.OPENED.name)).whenever(
            checkoutAccess,
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
            checkoutAccess,
        )
            .createOrder(any())

        val cause = createFeignNotFoundException(com.wutsi.marketplace.access.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn)
        doThrow(cause).whenever(marketplaceAccessApi).createReservation(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CreateOrderResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(checkoutAccess).createOrder(any())
        verify(marketplaceAccessApi).createReservation(any())

        verify(eventStream, never()).publish(any(), any())

        val response = ObjectMapper().readValue(ex.responseBodyAsString, ErrorResponse::class.java)
        assertEquals(com.wutsi.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn, response.error.code)
    }

    private fun url(): String = "http://localhost:$port/v1/orders"
}
