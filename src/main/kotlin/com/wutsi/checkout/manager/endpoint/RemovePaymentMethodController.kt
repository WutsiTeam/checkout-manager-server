package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.RemovePaymentMethodDelegate
import org.springframework.web.bind.`annotation`.DeleteMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RestController
import kotlin.String

@RestController
public class RemovePaymentMethodController(
    public val `delegate`: RemovePaymentMethodDelegate,
) {
    @DeleteMapping("/v1/payment-methods/{token}")
    public fun invoke(@PathVariable(name = "token") token: String) {
        delegate.invoke(token)
    }
}
