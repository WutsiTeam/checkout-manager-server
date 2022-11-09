package com.wutsi.checkout.manager.config

import com.wutsi.regulation.CountryRegulations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CountryRegulationConfiguration {
    @Bean
    fun countryRegulation() = CountryRegulations()
}
