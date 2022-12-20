package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.GetPaymentMethodDelegate
import com.wutsi.checkout.manager.dto.GetPaymentMethodResponse
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RestController
import kotlin.String

@RestController
public class GetPaymentMethodController(
    public val `delegate`: GetPaymentMethodDelegate,
) {
    @GetMapping("/v1/payment-methods/{token}")
    public fun invoke(@PathVariable(name = "token") token: String): GetPaymentMethodResponse =
        delegate.invoke(token)
}
