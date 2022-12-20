package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.CreateChargeDelegate
import com.wutsi.checkout.manager.dto.CreateChargeRequest
import com.wutsi.checkout.manager.dto.CreateChargeResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class CreateChargeController(
    public val `delegate`: CreateChargeDelegate,
) {
    @PostMapping("/v1/transactions/charge")
    public fun invoke(
        @Valid @RequestBody
        request: CreateChargeRequest,
    ): CreateChargeResponse =
        delegate.invoke(request)
}
