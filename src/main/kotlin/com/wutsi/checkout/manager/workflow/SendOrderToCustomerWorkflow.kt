package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.mail.MailMapper
import com.wutsi.enums.ProductType
import com.wutsi.mail.MailFilterSet
import com.wutsi.marketplace.access.dto.ProductSummary
import com.wutsi.marketplace.access.dto.SearchProductRequest
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.platform.core.messaging.Party
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.Country
import com.wutsi.regulation.RegulationEngine
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class SendOrderToCustomerWorkflow(
    eventStream: EventStream,
    private val mapper: MailMapper,
    private val templateEngine: TemplateEngine,
    private val regulationEngine: RegulationEngine,
    private val mailFilterSet: MailFilterSet
) : AbstractSendOrderWorkflow(eventStream) {
    override fun createMessage(order: Order, merchant: Account, type: MessagingType): Message? =
        when (type) {
            MessagingType.EMAIL -> Message(
                recipient = Party(
                    email = order.customerEmail,
                    displayName = order.customerName
                ),
                subject = getText("email.notify-customer.subject"),
                body = generateBody(order, merchant),
                mimeType = "text/html"
            )
            else -> null
        }

    private fun generateBody(order: Order, merchant: Account): String {
        val ctx = Context(LocaleContextHolder.getLocale())
        val country = regulationEngine.country(order.business.country)
        val products = marketplaceAccessApi.searchProduct(
            request = SearchProductRequest(
                productIds = order.items.map { it.productId },
                limit = order.items.size
            )
        ).products

        loadEvents(products, country, ctx)
        ctx.setVariable("order", mapper.toOrderModel(order, country))

        val body = templateEngine.process("order-customer.html", ctx)
        return mailFilterSet.filter(
            body = body,
            context = createMailContext(merchant)
        )
    }

    private fun loadEvents(products: List<ProductSummary>, country: Country, ctx: Context) {
        val xproducts = products.filter { it.type == ProductType.EVENT.name }
            .map { mapper.toProduct(it, country) }

        if (xproducts.isNotEmpty()) {
            ctx.setVariable("productEvents", xproducts)
        }
    }
}
