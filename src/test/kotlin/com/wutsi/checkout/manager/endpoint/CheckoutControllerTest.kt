package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.CreateChargeRequest
import com.wutsi.checkout.access.dto.CreateOrderItemRequest
import com.wutsi.checkout.access.dto.CreateOrderRequest
import com.wutsi.checkout.access.dto.CreateOrderResponse
import com.wutsi.checkout.access.dto.GetBusinessResponse
import com.wutsi.checkout.access.dto.GetOrderResponse
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.CheckoutRequest
import com.wutsi.checkout.manager.dto.CheckoutResponse
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.enums.ChannelType
import com.wutsi.enums.DeviceType
import com.wutsi.enums.OfferType
import com.wutsi.enums.PaymentMethodType
import com.wutsi.event.CheckoutEventPayload
import com.wutsi.marketplace.access.dto.CreateReservationRequest
import com.wutsi.marketplace.access.dto.CreateReservationResponse
import com.wutsi.marketplace.access.dto.GetProductResponse
import com.wutsi.marketplace.access.dto.ReservationItem
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.payment.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CheckoutControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    private val businessAccountId = 33333L
    private val productId = 1L
    private val reservationId = 11L
    private val orderId = "1111"
    private val product = Fixtures.createProduct(productId, pictures = listOf(Fixtures.createPictureSummary(id = 1)))
    private var order = Fixtures.createOrder(id = orderId, totalPrice = 15000)
    private val businessAccount =
        Fixtures.createAccount(id = businessAccountId, businessId = BUSINESS_ID, business = true)
    private val business = Fixtures.createBusiness(id = BUSINESS_ID, accountId = businessAccountId)
    private var transactionResponse = Fixtures.createChargeResponse()
    private val request = CheckoutRequest(
        channelType = ChannelType.WEBAPP.name,
        deviceType = DeviceType.MOBILE.name,
        businessId = BUSINESS_ID,
        idempotencyKey = UUID.randomUUID().toString(),
        paymentProviderId = 1000L,
        paymentMethodToken = null,
        quantity = 10,
        productId = productId,
        paymenMethodNumber = "+237670000010",
        customerId = 1111L,
        customerName = "Ray Sponsible",
        customerEmail = "ray.sponsible@gmail.com",
        paymentMethodType = PaymentMethodType.MOBILE_MONEY.name,
        notes = "This is a message to merchant :-)"
    )

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(GetAccountResponse(businessAccount)).whenever(membershipAccess).getAccount(businessAccountId)

        doReturn(GetProductResponse(product)).whenever(marketplaceAccessApi).getProduct(productId)

        doReturn(CreateReservationResponse(reservationId)).whenever(marketplaceAccessApi).createReservation(any())

        doReturn(GetBusinessResponse(business)).whenever(checkoutAccess).getBusiness(BUSINESS_ID)

        doReturn(CreateOrderResponse(order.id)).whenever(checkoutAccess).createOrder(any())
        doReturn(GetOrderResponse(order)).whenever(checkoutAccess).getOrder(orderId)

        doReturn(transactionResponse).whenever(checkoutAccess).createCharge(any())
    }

    @Test
    public fun pending() {
        // WHEN
        val response = rest.postForEntity(url(), request, CheckoutResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createOrder(
            request = CreateOrderRequest(
                deviceType = request.deviceType,
                channelType = request.channelType,
                customerEmail = request.customerEmail,
                customerId = request.customerId,
                notes = request.notes,
                customerName = request.customerName,
                businessId = business.id,
                currency = business.currency,
                items = listOf(
                    CreateOrderItemRequest(
                        offerId = productId,
                        offerType = OfferType.PRODUCT.name,
                        title = product.title,
                        pictureUrl = product.thumbnail?.url,
                        quantity = request.quantity,
                        unitPrice = product.price ?: 0
                    )
                )
            )
        )

        verify(marketplaceAccessApi).createReservation(
            request = CreateReservationRequest(
                orderId = order.id,
                items = listOf(
                    ReservationItem(
                        productId = request.productId,
                        quantity = request.quantity
                    )
                )
            )
        )

        verify(checkoutAccess).createCharge(
            request = CreateChargeRequest(
                email = request.customerEmail,
                paymentMethodType = request.paymentMethodType,
                paymenMethodNumber = request.paymenMethodNumber,
                paymentMethodToken = request.paymentMethodToken,
                businessId = business.id,
                paymentProviderId = request.paymentProviderId,
                idempotencyKey = request.idempotencyKey,
                amount = order.totalPrice,
                paymentMethodOwnerName = request.customerName,
                orderId = order.id
            )
        )

        val result = response.body!!
        assertEquals(transactionResponse.transactionId, result.transactionId)
        assertEquals(transactionResponse.status, result.transactionStatus)
        assertEquals(order.id, result.orderId)

        verify(eventStream, never()).publish(any(), any())
        verify(eventStream, never()).enqueue(any(), any())
    }

    @Test
    public fun success() {
        // GIVEN
        transactionResponse = Fixtures.createChargeResponse(status = Status.SUCCESSFUL)
        doReturn(transactionResponse).whenever(checkoutAccess).createCharge(any())

        // WHEN
        val response = rest.postForEntity(url(), request, CheckoutResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).createOrder(
            request = CreateOrderRequest(
                deviceType = request.deviceType,
                channelType = request.channelType,
                customerEmail = request.customerEmail,
                customerId = request.customerId,
                notes = request.notes,
                customerName = request.customerName,
                businessId = business.id,
                currency = business.currency,
                items = listOf(
                    CreateOrderItemRequest(
                        offerId = productId,
                        offerType = OfferType.PRODUCT.name,
                        title = product.title,
                        pictureUrl = product.thumbnail?.url,
                        quantity = request.quantity,
                        unitPrice = product.price ?: 0
                    )
                )
            )
        )

        verify(marketplaceAccessApi).createReservation(
            request = CreateReservationRequest(
                orderId = order.id,
                items = listOf(
                    ReservationItem(
                        productId = request.productId,
                        quantity = request.quantity
                    )
                )
            )
        )

        verify(checkoutAccess).createCharge(
            request = CreateChargeRequest(
                email = request.customerEmail,
                paymentMethodType = request.paymentMethodType,
                paymenMethodNumber = request.paymenMethodNumber,
                paymentMethodToken = request.paymentMethodToken,
                businessId = business.id,
                paymentProviderId = request.paymentProviderId,
                idempotencyKey = request.idempotencyKey,
                amount = order.totalPrice,
                paymentMethodOwnerName = request.customerName,
                orderId = order.id
            )
        )

        val result = response.body!!
        assertEquals(transactionResponse.transactionId, result.transactionId)
        assertEquals(transactionResponse.status, result.transactionStatus)
        assertEquals(order.id, result.orderId)

        verify(eventStream, never()).publish(any(), any())
        verify(eventStream).enqueue(
            InternalEventURN.CHARGE_SUCESSFULL.urn,
            CheckoutEventPayload(order.id, reservationId)
        )
    }

    @Test
    public fun free() {
        // GIVEN
        order = Fixtures.createOrder(id = orderId, totalPrice = 0)
        doReturn(GetOrderResponse(order)).whenever(checkoutAccess).getOrder(any())
        // WHEN
        val response = rest.postForEntity(url(), request, CheckoutResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(marketplaceAccessApi).checkProductAvailability(any())
        verify(checkoutAccess).createOrder(any())
        verify(marketplaceAccessApi).createReservation(any())
        verify(checkoutAccess, never()).createCharge(any())

        verify(eventStream, never()).publish(any(), any())
        verify(eventStream).enqueue(
            InternalEventURN.CHARGE_SUCESSFULL.urn,
            CheckoutEventPayload(order.id, reservationId)
        )
    }

    @Test
    fun `availability error`() {
        // GIVEN
        val cause = createFeignNotFoundException(com.wutsi.marketplace.access.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn)
        doThrow(cause).whenever(marketplaceAccessApi).checkProductAvailability(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CheckoutResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(checkoutAccess, never()).createOrder(any())
        verify(marketplaceAccessApi, never()).createReservation(any())
        verify(checkoutAccess, never()).createCharge(any())

        verify(eventStream, never()).publish(any(), any())
        verify(eventStream, never()).enqueue(any(), any())
    }

    @Test
    fun `reservation error`() {
        // GIVEN
        val cause = createFeignNotFoundException(com.wutsi.marketplace.access.error.ErrorURN.PRODUCT_NOT_AVAILABLE.urn)
        doThrow(cause).whenever(marketplaceAccessApi).createReservation(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CheckoutResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(marketplaceAccessApi).checkProductAvailability(any())
        verify(checkoutAccess).createOrder(any())
        verify(checkoutAccess, never()).createCharge(any())

        verify(eventStream, never()).publish(any(), any())
        verify(eventStream, never()).enqueue(any(), any())
    }

    @Test
    fun `charge error`() {
        // GIVEN
        val cause = createFeignConflictException(ErrorURN.TRANSACTION_FAILED.urn)
        doThrow(cause).whenever(checkoutAccess).createCharge(any())

        // WHEN
        val ex = assertThrows<HttpClientErrorException> {
            rest.postForEntity(url(), request, CheckoutResponse::class.java)
        }

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)

        verify(marketplaceAccessApi).checkProductAvailability(any())
        verify(checkoutAccess).createOrder(any())
        verify(marketplaceAccessApi).createReservation(any())
        verify(checkoutAccess).createCharge(any())

        verify(eventStream, never()).publish(any(), any())
        verify(eventStream).enqueue(InternalEventURN.CHARGE_FAILED.urn, CheckoutEventPayload(order.id, reservationId))
    }

    private fun url(): String = "http://localhost:$port/v1/checkout"
}
