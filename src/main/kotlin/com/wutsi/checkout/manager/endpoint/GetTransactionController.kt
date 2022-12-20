package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.GetTransactionDelegate
import com.wutsi.checkout.manager.dto.GetTransactionResponse
import org.springframework.web.bind.`annotation`.GetMapping
import org.springframework.web.bind.`annotation`.PathVariable
import org.springframework.web.bind.`annotation`.RequestParam
import org.springframework.web.bind.`annotation`.RestController
import kotlin.Boolean
import kotlin.String

@RestController
public class GetTransactionController(
    public val `delegate`: GetTransactionDelegate,
) {
    @GetMapping("/v1/transactions/{id}")
    public fun invoke(
        @PathVariable(name = "id") id: String,
        @RequestParam(name = "sync", required = false)
        sync: Boolean? = null,
    ): GetTransactionResponse = delegate.invoke(id, sync)
}
