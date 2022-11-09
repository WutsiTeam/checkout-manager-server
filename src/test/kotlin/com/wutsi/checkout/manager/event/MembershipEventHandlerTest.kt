package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.SearchPaymentProviderResponse
import com.wutsi.checkout.access.enums.PaymentMethodType
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.workflow.AddPaymentMethodWorkflow
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.membership.manager.event.EventURN
import com.wutsi.membership.manager.event.MemberEventPayload
import com.wutsi.platform.core.stream.Event
import com.wutsi.workflow.WorkflowContext
import org.junit.jupiter.api.Assertions.assertEquals
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
    private lateinit var workflow: AddPaymentMethodWorkflow

    @Autowired
    private lateinit var handler: MembershipEventHandler

    val account = Fixtures.createAccount(phoneNumber = "+237670000010")

    val payload = MemberEventPayload(
        phoneNumber = account.phone.number,
        accountId = 123L,
        pin = "111111"
    )
    val event = Event(
        type = EventURN.MEMBER_REGISTRATION_STARTED.urn,
        payload = ObjectMapper().writeValueAsString(payload),
        timestamp = OffsetDateTime.now()
    )

    @Test
    fun onMemberRegistered() {
        // GIVEN
        val providers = listOf(
            Fixtures.createPaymentProvider()
        )
        doReturn(SearchPaymentProviderResponse(providers)).whenever(checkoutAccessApi).searchPaymentProvider(any())

        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        // WHEN
        handler.onMemberRegistered(event)

        // THEN
        val context = argumentCaptor<WorkflowContext>()
        verify(workflow).execute(context.capture())

        val request = context.firstValue.request as AddPaymentMethodRequest
        assertEquals(account.phone.country, request.country)
        assertEquals(account.phone.number, request.number)
        assertEquals(PaymentMethodType.MOBILE_MONEY.name, request.type)
        assertEquals(account.phone.country, request.country)
        assertEquals(providers[0].id, request.providerId)
        assertEquals(account.displayName, request.ownerName)
    }

    @Test
    fun providerNotSupported() {
        // GIVEN
        doReturn(SearchPaymentProviderResponse()).whenever(checkoutAccessApi).searchPaymentProvider(any())

        val account = Fixtures.createAccount(phoneNumber = "+237670000010")
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        // WHEN
        handler.onMemberRegistered(event)

        // THEN
        verify(workflow, never()).execute(any())
    }

    @Test
    fun multipleProviders() {
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
        verify(workflow, never()).execute(any())
    }
}
