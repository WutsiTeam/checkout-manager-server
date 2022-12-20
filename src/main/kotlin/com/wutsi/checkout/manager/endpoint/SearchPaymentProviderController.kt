package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.SearchPaymentProviderDelegate
import com.wutsi.checkout.manager.dto.SearchPaymentProviderRequest
import com.wutsi.checkout.manager.dto.SearchPaymentProviderResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class SearchPaymentProviderController(
    public val `delegate`: SearchPaymentProviderDelegate,
) {
    @PostMapping("/v1/payment-providers/search")
    public fun invoke(
        @Valid @RequestBody
        request: SearchPaymentProviderRequest,
    ):
        SearchPaymentProviderResponse = delegate.invoke(request)
}
