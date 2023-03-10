package com.wutsi.checkout.manager.endpoint

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.membership.access.MembershipAccessApi
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate
import java.nio.charset.Charset

abstract class AbstractController2Test {
    @LocalServerPort
    val port: Int = 0

    @MockBean
    protected lateinit var marketplaceAccessApi: MarketplaceAccessApi

    @MockBean
    protected lateinit var membershipAccessApi: MembershipAccessApi

    @MockBean
    protected lateinit var checkoutAccessApi: CheckoutAccessApi

    protected var rest = RestTemplate()

    protected fun createFeignNotFoundException(code: String) = FeignException.NotFound(
        "",
        createFeignRequest(),
        createFeignBody(code),
        emptyMap(),
    )

    protected fun createFeignConflictException(code: String) = FeignException.Conflict(
        "",
        createFeignRequest(),
        createFeignBody(code),
        emptyMap(),
    )

    private fun createFeignRequest() = Request.create(
        Request.HttpMethod.POST,
        "https://www.google.ca",
        emptyMap(),
        "".toByteArray(),
        Charset.defaultCharset(),
        RequestTemplate(),
    )

    private fun createFeignBody(code: String) =
        """
            {
                "error":{
                    "code": "$code"
                }
            }
        """.trimIndent().toByteArray()
}
