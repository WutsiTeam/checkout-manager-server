package com.wutsi.checkout.manager.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.MembershipAccessApiBuilder
import com.wutsi.platform.core.security.feign.FeignApiKeyRequestInterceptor
import com.wutsi.platform.core.security.feign.FeignAuthorizationRequestInterceptor
import com.wutsi.platform.core.tracing.feign.FeignTracingRequestInterceptor
import com.wutsi.platform.core.util.feign.Custom5XXErrorDecoder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import javax.annotation.PostConstruct

@Configuration
class MembershipAccessApiConfiguration(
    private val authorizationRequestInterceptor: FeignAuthorizationRequestInterceptor,
    private val tracingRequestInterceptor: FeignTracingRequestInterceptor,
    private val apiKeyInterceptor: FeignApiKeyRequestInterceptor,
    private val mapper: ObjectMapper,
    private val env: Environment,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(MembershipAccessApiConfiguration::class.java)
    }

    @PostConstruct
    fun init() {
        val environment = environment()
        LOGGER.info("Endpoint: ${environment.name} = ${environment.url}")
    }

    @Bean
    fun membershipAccessApi(): MembershipAccessApi =
        MembershipAccessApiBuilder().build(
            env = environment(),
            mapper = mapper,
            interceptors = listOf(
                tracingRequestInterceptor,
                authorizationRequestInterceptor,
                apiKeyInterceptor,
            ),
            errorDecoder = Custom5XXErrorDecoder(),
        )

    private fun environment(): com.wutsi.membership.access.Environment =
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            com.wutsi.membership.access.Environment.PRODUCTION
        } else {
            com.wutsi.membership.access.Environment.SANDBOX
        }
}
