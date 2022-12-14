package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.GetOrderDelegate
import com.wutsi.checkout.manager.dto.GetOrderResponse
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RestController
import kotlin.String

@RestController
public class GetOrderController(
    public val `delegate`: GetOrderDelegate,
) {
    @GetMapping("/v1/orders/{id}")
    public fun invoke(@PathVariable(name = "id") id: String): GetOrderResponse = delegate.invoke(id)
}
