package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.SearchPaymentMethodDelegate
import com.wutsi.checkout.manager.dto.SearchPaymentMethodRequest
import com.wutsi.checkout.manager.dto.SearchPaymentMethodResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class SearchPaymentMethodController(
    public val `delegate`: SearchPaymentMethodDelegate,
) {
    @PostMapping("/v1/payment-methods/search")
    public fun invoke(
        @Valid @RequestBody
        request: SearchPaymentMethodRequest,
    ):
        SearchPaymentMethodResponse = delegate.invoke(request)
}
