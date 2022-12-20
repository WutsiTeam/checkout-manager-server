package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.CreateOrderDelegate
import com.wutsi.checkout.manager.dto.CreateOrderRequest
import com.wutsi.checkout.manager.dto.CreateOrderResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class CreateOrderController(
    public val `delegate`: CreateOrderDelegate,
) {
    @PostMapping("/v1/orders")
    public fun invoke(
        @Valid @RequestBody
        request: CreateOrderRequest,
    ): CreateOrderResponse =
        delegate.invoke(request)
}
