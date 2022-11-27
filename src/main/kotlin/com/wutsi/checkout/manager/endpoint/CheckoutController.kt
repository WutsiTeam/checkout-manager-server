package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.CheckoutDelegate
import com.wutsi.checkout.manager.dto.CheckoutRequest
import com.wutsi.checkout.manager.dto.CheckoutResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class CheckoutController(
    public val `delegate`: CheckoutDelegate
) {
    @PostMapping("/v1/checkout")
    public fun invoke(
        @Valid @RequestBody
        request: CheckoutRequest
    ): CheckoutResponse =
        delegate.invoke(request)
}
