package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetTransactionResponse
import com.wutsi.checkout.access.error.ErrorURN
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.TransactionType
import com.wutsi.platform.payment.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetTransactionControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    val tx =
        Fixtures.createTransaction("1111", type = TransactionType.CHARGE, status = Status.FAILED, orderId = "43434")

    @BeforeEach
    override fun setUp() {
        super.setUp()
        doReturn(GetTransactionResponse(tx)).whenever(checkoutAccess).getTransaction(any())
    }

    @Test
    fun get() {
        // WHEN
        val response = rest.getForEntity(url(tx.id), com.wutsi.checkout.manager.dto.GetTransactionResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        val value = response.body!!.transaction
        assertEquals(tx.id, value.id)
        assertEquals(tx.customerAccountId, value.customerAccountId)
        assertEquals(tx.amount, value.amount)
        assertEquals(tx.type, value.type)
        assertEquals(tx.gatewayType, value.gatewayType)
        assertEquals(tx.gatewayFees, value.gatewayFees)
        assertEquals(tx.gatewayTransactionId, value.gatewayTransactionId)
        assertEquals(tx.financialTransactionId, value.financialTransactionId)
        assertEquals(tx.description, value.description)
        assertEquals(tx.email, value.email)
        assertEquals(tx.errorCode, value.errorCode)
        assertEquals(tx.orderId, value.orderId)
        assertEquals(tx.status, value.status)
        assertEquals(tx.supplierErrorCode, value.supplierErrorCode)
        assertEquals(tx.business.id, value.business.id)
        assertEquals(tx.updated, value.updated)
        assertEquals(tx.created, value.created)
        assertEquals(tx.paymentMethod.status, value.paymentMethod.status)
        assertEquals(tx.paymentMethod.number, value.paymentMethod.number)
        assertEquals(tx.paymentMethod.ownerName, value.paymentMethod.ownerName)
        assertEquals(tx.paymentMethod.type, value.paymentMethod.type)
        assertEquals(tx.paymentMethod.token, value.paymentMethod.token)

        verify(checkoutAccess, never()).syncTransactionStatus(tx.id)
    }

    @Test
    fun syncAndGet() {
        // WHEN
        val response =
            rest.getForEntity(url(tx.id, true), com.wutsi.checkout.manager.dto.GetTransactionResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        val value = response.body!!.transaction
        assertEquals(tx.id, value.id)

        verify(checkoutAccess).syncTransactionStatus(tx.id)
    }

    @Test
    fun ignoreSyncError() {
        // GIVEN
        val ex = createFeignConflictException(ErrorURN.TRANSACTION_FAILED.urn)
        doThrow(ex).whenever(checkoutAccess).syncTransactionStatus(any())

        // WHEN
        val response =
            rest.getForEntity(url(tx.id, true), com.wutsi.checkout.manager.dto.GetTransactionResponse::class.java)

        // THEN
        assertEquals(HttpStatus.OK, response.statusCode)

        val value = response.body!!.transaction
        assertEquals(tx.id, value.id)
    }

    private fun url(id: String, sync: Boolean? = null) =
        if (sync != null) {
            "http://localhost:$port/v1/transactions/$id?sync=$sync"
        } else {
            "http://localhost:$port/v1/transactions/$id"
        }
}
