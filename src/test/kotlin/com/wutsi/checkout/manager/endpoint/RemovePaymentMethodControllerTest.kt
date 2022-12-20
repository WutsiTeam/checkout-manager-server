package com.wutsi.checkout.manager.endpoint

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.checkout.access.dto.GetPaymentMethodResponse
import com.wutsi.checkout.access.dto.UpdatePaymentMethodStatusRequest
import com.wutsi.checkout.manager.Fixtures
import com.wutsi.enums.PaymentMethodStatus
import com.wutsi.membership.access.dto.GetAccountResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RemovePaymentMethodControllerTest : AbstractSecuredControllerTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    public fun invoke() {
        // GIVEN
        val account = Fixtures.createAccount(id = ACCOUNT_ID)
        doReturn(GetAccountResponse(account)).whenever(membershipAccess).getAccount(ACCOUNT_ID)

        val paymentMethod = Fixtures.createPaymentMethod(token = "1111", accountId = ACCOUNT_ID)
        doReturn(GetPaymentMethodResponse(paymentMethod)).whenever(checkoutAccess).getPaymentMethod(any())

        // WHEN
        rest.delete(url(paymentMethod.token))

        // THEN
        checkoutAccess.updatePaymentMethodStatus(
            paymentMethod.token,
            UpdatePaymentMethodStatusRequest(status = PaymentMethodStatus.INACTIVE.name),
        )
    }

    private fun url(id: String) = "http://localhost:$port/v1/payment-methods/$id"
}
