package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.CreateCashoutDelegate
import com.wutsi.checkout.manager.dto.CreateCashoutRequest
import com.wutsi.checkout.manager.dto.CreateCashoutResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class CreateCashoutController(
    public val `delegate`: CreateCashoutDelegate,
) {
    @PostMapping("/v1/transactions/cashout")
    public fun invoke(
        @Valid @RequestBody
        request: CreateCashoutRequest,
    ): CreateCashoutResponse =
        delegate.invoke(request)
}
