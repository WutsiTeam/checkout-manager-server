package com.wutsi.checkout.manager.config

import com.wutsi.mail.MailFilterSet
import com.wutsi.mail.filter.CSSFilter
import com.wutsi.mail.filter.DecoratorFilter
import com.wutsi.mail.filter.UTMFilter
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.TemplateEngine
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver

@Configuration
class MailConfiguration(private val messageSource: MessageSource) {
    @Bean
    fun emailTemplateEngine(): TemplateEngine {
        val templateEngine = SpringTemplateEngine()
        templateEngine.addTemplateResolver(htmlTemplateResolver())
        templateEngine.setTemplateEngineMessageSource(messageSource)
        return templateEngine
    }

    @Bean
    fun mailFilterSet() = MailFilterSet(
        listOf(
            DecoratorFilter(),
            UTMFilter(),
            CSSFilter(),
        ),
    )

    private fun htmlTemplateResolver(): ITemplateResolver {
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.order = Integer.valueOf(2)
        templateResolver.prefix = "/templates/"
        templateResolver.suffix = ".html"
        templateResolver.templateMode = TemplateMode.HTML
        templateResolver.characterEncoding = Charsets.UTF_8.name()
        templateResolver.isCacheable = false
        return templateResolver
    }
}
