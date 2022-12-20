package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.SearchOrderDelegate
import com.wutsi.checkout.manager.dto.SearchOrderRequest
import com.wutsi.checkout.manager.dto.SearchOrderResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class SearchOrderController(
    public val `delegate`: SearchOrderDelegate,
) {
    @PostMapping("/v1/orders/search")
    public fun invoke(
        @Valid @RequestBody
        request: SearchOrderRequest,
    ): SearchOrderResponse =
        delegate.invoke(request)
}
