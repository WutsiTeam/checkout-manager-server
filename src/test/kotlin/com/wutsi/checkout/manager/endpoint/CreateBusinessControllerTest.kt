package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.CreateBusinessResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.CreateBusinessRequest
import com.wutsi.membership.access.dto.EnableBusinessRequest
import com.wutsi.membership.access.dto.GetAccountResponse
import com.wutsi.membership.access.dto.UpdateAccountAttributeRequest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CreateBusinessControllerTest : AbstractSecuredController2Test() {
    private val request = CreateBusinessRequest(
        cityId = 99999L,
        displayName = "Yo Man",
        whatsapp = true,
        biography = "This is a description",
        categoryId = 1213232L,
        email = "info@fake-news.com",
    )

    private val businessId = 333L

    @Test
    public fun invoke() {
        // GIVEN
        val account = Fixtures.createAccount(id = ACCOUNT_ID)
        doReturn(GetAccountResponse(account)).whenever(membershipAccessApi).getAccount(any())

        doReturn(CreateBusinessResponse(businessId)).whenever(checkoutAccessApi).createBusiness(any())

        // WHEN
        rest.postForEntity(url(), request, Any::class.java)

        // THEN
        val req = argumentCaptor<EnableBusinessRequest>()
        verify(membershipAccessApi).enableBusiness(eq(account.id), req.capture())
        assertEquals(account.country, req.firstValue.country)
        assertEquals(request.categoryId, req.firstValue.categoryId)
        assertEquals(request.cityId, req.firstValue.cityId)
        assertEquals(request.whatsapp, req.firstValue.whatsapp)
        assertEquals(request.displayName, req.firstValue.displayName)
        assertEquals(request.biography, req.firstValue.biography)
        assertEquals(request.email, req.firstValue.email)

        verify(checkoutAccessApi).createBusiness(
            com.wutsi.checkout.access.dto.CreateBusinessRequest(
                accountId = account.id,
                country = account.country,
                currency = "XAF",
            ),
        )

        Thread.sleep(20000) // Wait for async processing
        verify(membershipAccessApi).updateAccountAttribute(
            account.id,
            UpdateAccountAttributeRequest("business-id", businessId.toString()),
        )
    }

    private fun url() = "http://localhost:$port/v1/business"
}
