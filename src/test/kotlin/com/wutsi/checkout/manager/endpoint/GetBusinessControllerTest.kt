package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.GetBusinessResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GetBusinessControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    public val port: Int = 0

    fun url(id: Long) = "http://localhost:$port/v1/businesses/$id"

    @Test
    public fun invoke() {
        // GIVEN
        val business = Fixtures.createBusiness(BUSINESS_ID, ACCOUNT_ID)
        doReturn(com.wutsi.checkout.access.dto.GetBusinessResponse(business)).whenever(checkoutAccess).getBusiness(
            BUSINESS_ID,
        )

        // WHEN
        val response = rest.getForEntity(url(BUSINESS_ID), GetBusinessResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        val bizness = response.body!!.business
        assertEquals(business.id, bizness.id)
        assertEquals(business.accountId, bizness.accountId)
        assertEquals(business.country, bizness.country)
        assertEquals(business.currency, bizness.currency)
        assertEquals(business.balance, bizness.balance)
        assertEquals(business.balance, bizness.balance)
        assertEquals(business.status, bizness.status)
        assertEquals(business.totalOrders, bizness.totalOrders)
        assertEquals(business.totalSales, bizness.totalSales)
        assertEquals(business.cashoutBalance, bizness.cashoutBalance)
    }
}
