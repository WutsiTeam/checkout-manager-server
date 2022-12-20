package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.AddPaymentMethodDelegate
import com.wutsi.checkout.manager.dto.AddPaymentMethodRequest
import com.wutsi.checkout.manager.dto.AddPaymentMethodResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class AddPaymentMethodController(
    public val `delegate`: AddPaymentMethodDelegate,
) {
    @PostMapping("/v1/payment-methods")
    public fun invoke(
        @Valid @RequestBody
        request: AddPaymentMethodRequest,
    ): AddPaymentMethodResponse =
        delegate.invoke(request)
}
