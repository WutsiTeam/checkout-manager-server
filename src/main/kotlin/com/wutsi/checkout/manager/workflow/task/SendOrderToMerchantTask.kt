package com.wutsi.checkout.manager.workflow.task

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.mail.Mapper
import com.wutsi.mail.MailFilterSet
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.platform.core.messaging.Party
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.WorkflowContext
import com.wutsi.workflow.util.WorkflowIdGenerator
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale

@Service
class SendOrderToMerchantTask(
    private val mapper: Mapper,
    private val templateEngine: TemplateEngine,
    private val regulationEngine: RegulationEngine,
    private val mailFilterSet: MailFilterSet,
) : AbstractSendOrderTask() {
    companion object {
        val ID = WorkflowIdGenerator.generate("marketplace", "send-order-to-merchant")
    }

    override fun id() = ID

    override fun createMessage(
        order: Order,
        merchant: Account,
        type: MessagingType,
        context: WorkflowContext,
    ): Message? {
        val locale = Locale(merchant.language)
        return when (type) {
            MessagingType.EMAIL -> merchant.email?.let {
                Message(
                    recipient = Party(
                        email = it,
                        displayName = merchant.displayName,
                    ),
                    subject = getText("email.notify-merchant.subject", locale = locale),
                    body = generateBody(order, merchant, locale, "wutsi"),
                    mimeType = "text/html;charset=UTF-8",
                )
            }
            MessagingType.PUSH_NOTIFICATION -> getDeviceToken(merchant)?.let {
                Message(
                    recipient = Party(
                        deviceToken = it,
                        displayName = merchant.displayName,
                    ),
                    body = getText("push-notification.notify-merchant.body"),
                )
            }
            else -> null
        }
    }

    private fun generateBody(order: Order, merchant: Account, locale: Locale, template: String? = null): String {
        val ctx = Context(locale)
        val country = regulationEngine.country(order.business.country)
        val mailContext = createMailContext(merchant, template)
        ctx.setVariable("order", mapper.toOrderModel(order, country))
        ctx.setVariable("merchant", mailContext.merchant)

        val body = templateEngine.process("order-merchant.html", ctx)
        return mailFilterSet.filter(
            body = body,
            context = mailContext,
        )
    }

    private fun getDeviceToken(merchant: Account): String? =
        try {
            membershipAccessApi.getAccountDevice(merchant.id).device.token
        } catch (ex: Exception) {
            null
        }
}
