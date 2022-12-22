package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.SearchTransactionResponse
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.checkout.manager.dto.SearchTransactionRequest
import com.wutsi.enums.TransactionType
import com.wutsi.platform.payment.core.Status
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchTransactionControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun invoke() {
        // GIVEN
        val txs = listOf(
            Fixtures.createTransactionSummary("111"),
            Fixtures.createTransactionSummary("222"),
            Fixtures.createTransactionSummary("333"),
        )
        doReturn(SearchTransactionResponse(txs)).whenever(checkoutAccess).searchTransaction(any())

        // WHEN
        val request = SearchTransactionRequest(
            customerAccountId = 111L,
            status = listOf(Status.SUCCESSFUL.name, Status.PENDING.name),
            offset = 111,
            limit = 10,
            orderId = "111",
            type = TransactionType.CHARGE.name,
            businessId = 3333L,
        )
        val response = rest.postForEntity(url(), request, SearchTransactionResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        verify(checkoutAccess).searchTransaction(
            request = com.wutsi.checkout.access.dto.SearchTransactionRequest(
                customerAccountId = request.customerAccountId,
                type = request.type,
                offset = request.offset,
                limit = request.limit,
                orderId = request.orderId,
                businessId = request.businessId,
                status = request.status,
            ),
        )

        val transactions = response.body!!.transactions
        assertEquals(txs.size, transactions.size)
    }

    private fun url() = "http://localhost:$port/v1/transactions/search"
}
