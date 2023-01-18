package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.CreatePaymentMethodRequest
import com.wutsi.checkout.access.dto.CreatePaymentMethodResponse
import com.wutsi.checkout.access.dto.GetPaymentMethodResponse
import com.wutsi.checkout.access.dto.SearchPaymentMethodResponse
import com.wutsi.checkout.access.dto.SearchPaymentProviderResponse
import com.wutsi.checkout.access.dto.UpdateBusinessStatusRequest
import com.wutsi.checkout.access.dto.UpdatePaymentMethodStatusRequest
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.BusinessStatus
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.event.BusinessEventPayload
import com.wutsi.event.EventURN
import com.wutsi.event.MemberEventPayload
import com.wutsi.event.PaymentMethodEventPayload
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.stream.EventStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class MembershipEventHandlerTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var membershipAccessApi: MembershipAccessApi

    @MockBean
    private lateinit var eventStream: EventStream

    @Autowired
    private lateinit var handler: MembershipEventHandler

    val account = Fixtures.createAccount(id = 123L, phoneNumber = "+237670000010")

    val payload = MemberEventPayload(
        phoneNumber = account.phone.number,
        accountId = 123L,
        pin = "111111",
    )
    val event = Event(
        payload = ObjectMapper().writeValueAsString(payload),
        timestamp = OffsetDateTime.now(),
    )

    @BeforeEach
    fun setUp() {
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())
    }

    @Test
    fun onMemberRegistered() {
        // GIVEN
        val paymentProvider = Fixtures.createPaymentProvider()
        doReturn(SearchPaymentProviderResponse(listOf(paymentProvider))).whenever(checkoutAccessApi)
            .searchPaymentProvider(any())

        val token = "Xxxx"
        doReturn(CreatePaymentMethodResponse(token)).whenever(checkoutAccessApi).createPaymentMethod(any())

        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        // WHEN
        handler.onMemberRegistered(event)

        // THEN
        verify(checkoutAccessApi).createPaymentMethod(
            CreatePaymentMethodRequest(
                accountId = account.id,
                ownerName = account.displayName,
                number = account.phone.number,
                country = account.phone.country,
                providerId = paymentProvider.id,
                type = PaymentMethodType.MOBILE_MONEY.name,
            ),
        )

        verify(eventStream).publish(
            EventURN.PAYMENT_METHOD_ADDED.urn,
            PaymentMethodEventPayload(
                accountId = account.id,
                paymentMethodToken = token,
            ),
        )
    }

    @Test
    fun onMemberRegisteredWithPaymentProviderNotSupported() {
        // GIVEN
        doReturn(SearchPaymentProviderResponse()).whenever(checkoutAccessApi).searchPaymentProvider(any())

        val account = Fixtures.createAccount(phoneNumber = "+237670000010")
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        // WHEN
        handler.onMemberRegistered(event)

        // THEN
        verify(checkoutAccessApi, never()).createPaymentMethod(any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun onMemberRegisteredWithMultiplePaymentProviders() {
        // GIVEN
        val providers = listOf(
            Fixtures.createPaymentProvider(type = PaymentMethodType.MOBILE_MONEY),
            Fixtures.createPaymentProvider(type = PaymentMethodType.BANK),
        )
        doReturn(SearchPaymentProviderResponse(providers)).whenever(checkoutAccessApi).searchPaymentProvider(any())

        val account = Fixtures.createAccount(phoneNumber = "+237670000010")
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        // WHEN
        handler.onMemberRegistered(event)

        // THEN
        verify(checkoutAccessApi, never()).createPaymentMethod(any())
        verify(eventStream, never()).publish(any(), any())
    }

    @Test
    fun onBusinessMemberDeleted() {
        // GIVEN
        val account = Fixtures.createAccount(id = payload.accountId, business = true, businessId = 222)
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        val paymentMethods = listOf(
            Fixtures.createPaymentMethodSummary(token = "111"),
            Fixtures.createPaymentMethodSummary(token = "222"),
        )
        doReturn(SearchPaymentMethodResponse(paymentMethods)).whenever(checkoutAccessApi).searchPaymentMethod(any())

        val paymentMethod1 = Fixtures.createPaymentMethod(paymentMethods[0].token, accountId = account.id)
        val paymentMethod2 = Fixtures.createPaymentMethod(paymentMethods[1].token, accountId = account.id)
        doReturn(GetPaymentMethodResponse(paymentMethod1)).whenever(checkoutAccessApi)
            .getPaymentMethod(paymentMethods[0].token)
        doReturn(GetPaymentMethodResponse(paymentMethod2)).whenever(checkoutAccessApi)
            .getPaymentMethod(paymentMethods[1].token)

        // WHEN
        handler.onMemberDeleted(event)

        // THEN
        verify(checkoutAccessApi).updateBusinessStatus(
            account.businessId!!,
            UpdateBusinessStatusRequest(status = BusinessStatus.INACTIVE.name),
        )

        val token = argumentCaptor<String>()
        verify(checkoutAccessApi, times(2)).updatePaymentMethodStatus(
            token.capture(),
            eq(UpdatePaymentMethodStatusRequest(status = PaymentMethodStatus.INACTIVE.name)),
        )
        assertEquals(paymentMethods[0].token, token.firstValue)
        assertEquals(paymentMethods[1].token, token.secondValue)

        verify(eventStream).publish(
            EventURN.BUSINESS_DEACTIVATED.urn,
            BusinessEventPayload(
                businessId = account.businessId!!,
                accountId = account.id,
            ),
        )
    }

    @Test
    fun onMemberDeleted() {
        // GIVEN
        val account = Fixtures.createAccount(id = payload.accountId, business = false, businessId = null)
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        val paymentMethods = listOf(
            Fixtures.createPaymentMethodSummary(token = "111"),
            Fixtures.createPaymentMethodSummary(token = "222"),
        )
        doReturn(SearchPaymentMethodResponse(paymentMethods)).whenever(checkoutAccessApi).searchPaymentMethod(any())

        val paymentMethod1 = Fixtures.createPaymentMethod(paymentMethods[0].token, accountId = account.id)
        val paymentMethod2 = Fixtures.createPaymentMethod(paymentMethods[1].token, accountId = account.id)
        doReturn(GetPaymentMethodResponse(paymentMethod1)).whenever(checkoutAccessApi)
            .getPaymentMethod(paymentMethods[0].token)
        doReturn(GetPaymentMethodResponse(paymentMethod2)).whenever(checkoutAccessApi)
            .getPaymentMethod(paymentMethods[1].token)

        // WHEN
        handler.onMemberDeleted(event)

        // THEN
        verify(checkoutAccessApi, never()).updateBusinessStatus(any(), any())

        val token = argumentCaptor<String>()
        verify(checkoutAccessApi, times(2)).updatePaymentMethodStatus(
            token.capture(),
            eq(UpdatePaymentMethodStatusRequest(status = PaymentMethodStatus.INACTIVE.name)),
        )
        assertEquals(paymentMethods[0].token, token.firstValue)
        assertEquals(paymentMethods[1].token, token.secondValue)

        verify(eventStream, never()).publish(any(), any())
    }
}
