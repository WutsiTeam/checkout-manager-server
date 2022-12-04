package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.mail.MailMapper
import com.wutsi.event.OrderEventPayload
import com.wutsi.mail.MailContext
import com.wutsi.mail.MailFilterSet
import com.wutsi.mail.Merchant
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingServiceProvider
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.platform.core.messaging.Party
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.RuleSet
import com.wutsi.workflow.WorkflowContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
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
    private val messagingServiceProvider: MessagingServiceProvider,
    private val messages: MessageSource,
    private val mailFilterSet: MailFilterSet,
    private val logger: KVLogger,

    @Value("\${wutsi.application.asset-url}") private val assetUrl: String,
    @Value("\${wutsi.application.webapp-url}") private val webappUrl: String
) : AbstractOrderWorkflow<String, Unit>(eventStream) {
    override fun getEventType(): String? = null

    override fun toEventPayload(orderId: String, response: Unit, context: WorkflowContext): OrderEventPayload? = null

    override fun getValidationRules(orderId: String, context: WorkflowContext) = RuleSet.NONE

    override fun doExecute(orderId: String, context: WorkflowContext) {
        val order = checkoutAccessApi.getOrder(orderId).order
        logger.add("order_customer_name", order.customerName)
        logger.add("order_customer_email", order.customerEmail)
        logger.add("merchant_id", order.business.accountId)

        val merchant = membershipAccessApi.getAccount(order.business.accountId).account

        // Send email
        val emailMessageId = sendEmail(order, merchant)
        logger.add("message_id_email", emailMessageId)
    }

    private fun sendEmail(order: Order, merchant: Account): String {
        val messaging = messagingServiceProvider.get(MessagingType.EMAIL)
        val locale = LocaleContextHolder.getLocale()
        return messaging.send(
            message = Message(
                recipient = Party(
                    email = order.customerEmail,
                    displayName = order.customerName
                ),
                subject = messages.getMessage("email.notify-customer.subject", emptyArray(), locale),
                body = generateBody(order, merchant),
                mimeType = "text/html"
            )
        )
    }

    private fun generateBody(order: Order, merchant: Account): String {
        val ctx = Context(LocaleContextHolder.getLocale())
        val country = regulationEngine.country(order.business.country)
        ctx.setVariable("order", mapper.toOrderModel(order, country))

        val body = templateEngine.process("order-customer.html", ctx)
        return mailFilterSet.filter(
            body = body,
            context = MailContext(
                assetUrl = assetUrl,
                merchant = Merchant(
                    url = "$webappUrl/u/${merchant.id}",
                    name = merchant.displayName,
                    logoUrl = merchant.pictureUrl,
                    category = merchant.category?.title,
                    location = merchant.city?.longName
                )
            )
        )
    }
}
