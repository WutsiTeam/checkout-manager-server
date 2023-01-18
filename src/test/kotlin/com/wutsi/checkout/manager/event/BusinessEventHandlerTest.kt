package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.CreateBusinessRequest
import com.wutsi.checkout.access.dto.CreateBusinessResponse
import com.wutsi.checkout.access.dto.UpdateBusinessStatusRequest
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.BusinessStatus
import com.wutsi.event.BusinessEventPayload
import com.wutsi.event.EventURN
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class BusinessEventHandlerTest {
    @MockBean
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @MockBean
    private lateinit var membershipAccessApi: MembershipAccessApi

    @MockBean
    private lateinit var eventStream: EventStream

    @Autowired
    private lateinit var handler: BusinessEventHandler

    val account = Fixtures.createAccount(id = 123L, phoneNumber = "+237670000010", businessId = 333L)

    val payload = BusinessEventPayload(
        accountId = account.id,
        businessId = account.businessId!!,
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
    fun onBusinessSuspended() {
        // WHEN
        handler.onBusinessDeactivated(event)

        // THEN
        verify(checkoutAccessApi).updateBusinessStatus(
            payload.businessId,
            UpdateBusinessStatusRequest(
                status = BusinessStatus.INACTIVE.name,
            ),
        )

        verify(eventStream).publish(
            EventURN.BUSINESS_DEACTIVATED.urn,
            BusinessEventPayload(
                businessId = payload.businessId,
                accountId = payload.accountId,
            ),
        )
    }

    @Test
    fun onBusinessEnabled() {
        // GIVEN
        val businessId = 1111L
        doReturn(CreateBusinessResponse(businessId)).whenever(checkoutAccessApi).createBusiness(any())

        // WHEN
        handler.onBusinessActivated(event)

        // THEN
        verify(checkoutAccessApi).createBusiness(
            CreateBusinessRequest(
                accountId = account.id,
                country = account.country,
                currency = "XAF",
            ),
        )

        verify(eventStream).publish(
            EventURN.BUSINESS_CREATED.urn,
            BusinessEventPayload(
                businessId = businessId,
                accountId = account.id,
            ),
        )
    }
}
