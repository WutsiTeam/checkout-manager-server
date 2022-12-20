package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.UpdateOrderStatusDelegate
import com.wutsi.checkout.manager.dto.UpdateOrderStatusRequest
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class UpdateOrderStatusController(
    public val `delegate`: UpdateOrderStatusDelegate,
) {
    @PostMapping("/v1/orders/status")
    public fun invoke(
        @Valid @RequestBody
        request: UpdateOrderStatusRequest,
    ) {
        delegate.invoke(request)
    }
}
