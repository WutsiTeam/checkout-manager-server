package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.SearchTransactionDelegate
import com.wutsi.checkout.manager.dto.SearchTransactionRequest
import com.wutsi.checkout.manager.dto.SearchTransactionResponse
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class SearchTransactionController(
    public val `delegate`: SearchTransactionDelegate,
) {
    @PostMapping("/v1/transactions/search")
    public fun invoke(
        @Valid @RequestBody
        request: SearchTransactionRequest,
    ):
        SearchTransactionResponse = delegate.invoke(request)
}
