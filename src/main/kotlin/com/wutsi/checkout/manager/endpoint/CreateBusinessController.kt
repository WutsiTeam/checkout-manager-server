package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.manager.`delegate`.CreateBusinessDelegate
import com.wutsi.checkout.manager.dto.CreateBusinessRequest
import org.springframework.web.bind.`annotation`.PostMapping
import org.springframework.web.bind.`annotation`.RequestBody
import org.springframework.web.bind.`annotation`.RestController
import javax.validation.Valid

@RestController
public class CreateBusinessController(
    public val `delegate`: CreateBusinessDelegate,
) {
    @PostMapping("/v1/business")
    public fun invoke(
        @Valid @RequestBody
        request: CreateBusinessRequest,
    ) {
        delegate.invoke(request)
    }
}
