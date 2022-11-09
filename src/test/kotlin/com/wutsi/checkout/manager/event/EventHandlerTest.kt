package com.wutsi.checkout.manager.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.verify
import com.wutsi.membership.manager.event.EventURN
import com.wutsi.membership.manager.event.MemberEventPayload
import com.wutsi.platform.core.stream.Event
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class EventHandlerTest {
    @MockBean
    private lateinit var membership: MembershipEventHandler

    @Autowired
    private lateinit var handler: EventHandler

    @Autowired
    private lateinit var mapper: ObjectMapper

    private val memberEventPayload = MemberEventPayload(
        phoneNumber = "+237670000010",
        accountId = 111L,
        pin = "123456"
    )

    @Test
    fun onMemberRegistered() {
        // WHEN
        val event = Event(
            type = EventURN.MEMBER_REGISTERED.urn,
            payload = mapper.writeValueAsString(memberEventPayload)
        )
        handler.handleEvent(event)

        // THEN
        verify(membership).onMemberRegistered(event)
    }

    @Test
    fun onBusinessEnabled() {
        // WHEN
        val event = Event(
            type = EventURN.BUSINESS_ACCOUNT_ENABLED.urn,
            payload = mapper.writeValueAsString(memberEventPayload)
        )
        handler.handleEvent(event)

        // THEN
        verify(membership).onBusinessAccountEnabled(event)
    }

    @Test
    fun onBusinessDisabled() {
        // WHEN
        val event = Event(
            type = EventURN.BUSINESS_ACCOUNT_DISABLED.urn,
            payload = mapper.writeValueAsString(memberEventPayload)
        )
        handler.handleEvent(event)

        // THEN
        verify(membership).onBusinessAccountDisabled(event)
    }
}
