package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.checkout.manager.mail.MailMapper
import com.wutsi.enums.ProductType
import com.wutsi.event.OrderEventPayload
import com.wutsi.mail.MailFilterSet
import com.wutsi.marketplace.access.dto.SearchProductRequest
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.platform.core.messaging.Party
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.Country
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.WorkflowContext
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
    override fun createMessage(
        order: Order,
        merchant: Account,
        type: MessagingType,
        context: WorkflowContext
    ): Message? =
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

    override fun doExecute(orderId: String, context: WorkflowContext) {
        super.doExecute(orderId, context)

        if (isFulfilled(orderId, context)) {
            eventStream.enqueue(InternalEventURN.ORDER_FULLFILLED.urn, OrderEventPayload(orderId))
        }
    }

    private fun isFulfilled(orderId: String, context: WorkflowContext): Boolean {
        val order = getOrder(orderId, context)
        return order.items.all {
            ProductType.valueOf(it.productType).numeric
        }
    }

    private fun generateBody(order: Order, merchant: Account): String {
        val ctx = Context(LocaleContextHolder.getLocale())
        val country = regulationEngine.country(order.business.country)

        ctx.setVariable("order", mapper.toOrderModel(order, country))
        loadEvents(order, country, ctx)

        val body = templateEngine.process("order-customer.html", ctx)
        return mailFilterSet.filter(
            body = body,
            context = createMailContext(merchant)
        )
    }

    private fun loadEvents(order: Order, country: Country, ctx: Context) {
        val productIds = order.items
            .filter { it.productType == ProductType.EVENT.name }
            .map { it.productId }
        if (productIds.isEmpty()) {
            return
        }

        val products = marketplaceAccessApi.searchProduct(
            request = SearchProductRequest(
                productIds = productIds,
                limit = productIds.size
            )
        ).products
        if (products.isNotEmpty()) {
            ctx.setVariable("productEvents", products.map { mapper.toProduct(it, country) })
        }
    }
}
