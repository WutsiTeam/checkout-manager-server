package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.CreateBusinessRequest
import com.wutsi.checkout.access.dto.CreateBusinessResponse
import com.wutsi.checkout.access.dto.CreatePaymentMethodRequest
import com.wutsi.checkout.access.dto.CreatePaymentMethodResponse
import com.wutsi.checkout.access.dto.SearchPaymentProviderResponse
import com.wutsi.checkout.access.dto.UpdateBusinessStatusRequest
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.BusinessStatus
import com.wutsi.enums.PaymentMethodType
import com.wutsi.event.BusinessEventPayload
import com.wutsi.event.EventURN
import com.wutsi.event.MemberEventPayload
import com.wutsi.event.PaymentMethodEventPayload
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.membership.access.dto.UpdateAccountAttributeRequest
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.stream.EventStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.OffsetDateTime

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
        pin = "111111"
    )
    val event = Event(
        payload = ObjectMapper().writeValueAsString(payload),
        timestamp = OffsetDateTime.now()
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
                type = PaymentMethodType.MOBILE_MONEY.name
            )
        )

        verify(eventStream).publish(
            EventURN.PAYMENT_METHOD_ADDED.urn,
            PaymentMethodEventPayload(
                accountId = account.id,
                paymentMethodToken = token
            )
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
            Fixtures.createPaymentProvider(type = PaymentMethodType.BANK)
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
    fun onBusinessEnabled() {
        // GIVEN
        val businessId = 1111L
        doReturn(CreateBusinessResponse(businessId)).whenever(checkoutAccessApi).createBusiness(any())

        // WHEN
        handler.onBusinessAccountEnabled(event)

        // THEN
        verify(checkoutAccessApi).createBusiness(
            CreateBusinessRequest(
                accountId = account.id,
                country = account.country,
                currency = "XAF"
            )
        )

        verify(membershipAccessApi).updateAccountAttribute(
            account.id,
            UpdateAccountAttributeRequest(
                name = "business-id",
                value = businessId.toString()
            )
        )

        verify(eventStream).publish(
            EventURN.BUSINESS_CREATED.urn,
            BusinessEventPayload(
                businessId = businessId,
                accountId = account.id
            )
        )
    }

    @Test
    fun onBusinessDisabled() {
        // GIVEN
        val businessId = 111L
        val account = Fixtures.createAccount(id = 123L, phoneNumber = "+237670000010", businessId = businessId)
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        // WHEN
        handler.onBusinessAccountDisabled(event)

        // THEN
        verify(checkoutAccessApi).updateBusinessStatus(
            businessId,
            UpdateBusinessStatusRequest(
                status = BusinessStatus.SUSPENDED.name
            )
        )

        verify(membershipAccessApi).updateAccountAttribute(
            account.id,
            UpdateAccountAttributeRequest(
                name = "business-id",
                value = null
            )
        )

        verify(eventStream).publish(
            EventURN.BUSINESS_SUSPENDED.urn,
            BusinessEventPayload(
                businessId = businessId,
                accountId = account.id
            )
        )
    }

    @Test
    fun onBusinessDisabledWithAccountNotHavingBusinessId() {
        // WHEN
        handler.onBusinessAccountDisabled(event)

        // THEN
        verify(checkoutAccessApi, never()).updateBusinessStatus(any(), any())
        verify(membershipAccessApi, never()).updateAccountAttribute(any(), any())
        verify(eventStream, never()).publish(any(), any())
    }
}
