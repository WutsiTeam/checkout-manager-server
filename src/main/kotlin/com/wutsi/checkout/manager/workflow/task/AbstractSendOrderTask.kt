package com.wutsi.checkout.manager.workflow.task

import com.wutsi.checkout.access.CheckoutAccessApi
import com.wutsi.checkout.access.dto.Order
import com.wutsi.mail.MailContext
import com.wutsi.mail.Merchant
import com.wutsi.marketplace.access.MarketplaceAccessApi
import com.wutsi.membership.access.MembershipAccessApi
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingServiceProvider
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.engine.Workflow
import com.wutsi.workflow.engine.WorkflowEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import java.util.Locale
import javax.annotation.PostConstruct

abstract class AbstractSendOrderTask : Workflow {
    companion object {
        const val CONTEXT_ORDER_ID = "order-id"
    }

    @Value("\${wutsi.application.asset-url}")
    private lateinit var assetUrl: String

    @Value("\${wutsi.application.webapp-url}")
    private lateinit var webappUrl: String

    @Value("\${wutsi.application.notification.debug}")
    private lateinit var debugNodifications: String

    @Autowired
    private lateinit var messagingServiceProvider: MessagingServiceProvider

    @Autowired
    protected lateinit var logger: KVLogger

    @Autowired
    private lateinit var messages: MessageSource

    @Autowired
    protected lateinit var workflowEngine: WorkflowEngine

    @Autowired
    protected lateinit var membershipAccessApi: MembershipAccessApi

    @Autowired
    private lateinit var checkoutAccessApi: CheckoutAccessApi

    @Autowired
    protected lateinit var marketplaceAccessApi: MarketplaceAccessApi

    @PostConstruct
    fun init() {
        workflowEngine.register(id(), this)
    }

    protected abstract fun id(): String

    protected abstract fun createMessage(
        order: Order,
        merchant: Account,
        type: MessagingType,
        context: WorkflowContext,
    ): Message?

    override fun execute(context: WorkflowContext) {
        // Order
        val orderId = context.data[CONTEXT_ORDER_ID] as String

        val order = getOrder(orderId)
        logger.add("order_customer_name", order.customerName)
        logger.add("order_customer_email", order.customerEmail)
        logger.add("merchant_id", order.business.accountId)

        // Merchant
        val merchant = membershipAccessApi.getAccount(order.business.accountId).account

        // Send email
        createMessage(order, merchant, MessagingType.EMAIL, context)?.let {
            val messageId = sendEmail(message = debug(it))
            logger.add("message_id_email", messageId)
        }

        // Send push notification
        createMessage(order, merchant, MessagingType.PUSH_NOTIFICATION, context)?.let {
            try {
                val messageId = sendPushNotification(message = debug(it))
                logger.add("message_id_push", messageId)
            } catch (ex: Exception) {
                getLogger().warn("Unable to send push notification", ex)
            }
        }
    }

    protected fun getText(
        key: String,
        args: Array<Any> = emptyArray(),
        locale: Locale = LocaleContextHolder.getLocale(),
    ): String =
        messages.getMessage(key, args, locale)

    private fun debug(message: Message): Message {
        if (debugNodifications.toBoolean()) {
            val logger = getLogger()
            logger.info("Recipient Address: ${message.recipient.displayName}< ${message.recipient.email}>")
            message.recipient.deviceToken?.let {
                logger.info("Recipient Device: $it")
            }
            message.subject?.let {
                logger.info("Subject: $it")
            }
            logger.info("\n${message.body}\n")
        }
        return message
    }

    private fun getLogger(): Logger =
        LoggerFactory.getLogger(this::class.java)

    protected fun createMailContext(merchant: Account, template: String? = null) = MailContext(
        assetUrl = assetUrl,
        merchant = Merchant(
            url = if (merchant.name == null) "$webappUrl/u/${merchant.id}" else "$webappUrl/@${merchant.name}",
            name = merchant.displayName.uppercase(),
            logoUrl = merchant.pictureUrl,
            category = merchant.category?.title,
            location = merchant.city?.longName,
            phoneNumber = merchant.phone.number,
            whatsapp = merchant.whatsapp,
            websiteUrl = merchant.website,
            twitterId = merchant.twitterId,
            facebookId = merchant.facebookId,
            instagramId = merchant.instagramId,
            youtubeId = merchant.youtubeId,
            country = merchant.country,
        ),
        template = template,
    )

    private fun sendEmail(message: Message): String? {
        if (message.recipient.email.isNullOrEmpty()) {
            return null
        }
        val sender = messagingServiceProvider.get(MessagingType.EMAIL)
        return sender.send(message)
    }

    private fun sendPushNotification(message: Message): String? {
        if (message.recipient.deviceToken.isNullOrEmpty()) {
            return null
        }
        val sender = messagingServiceProvider.get(MessagingType.PUSH_NOTIFICATION)
        return sender.send(message)
    }

    protected fun getOrder(orderId: String): Order =
        checkoutAccessApi.getOrder(orderId).order
}
