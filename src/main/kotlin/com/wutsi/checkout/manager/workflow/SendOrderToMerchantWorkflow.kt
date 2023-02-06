package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.mail.Mapper
import com.wutsi.mail.MailFilterSet
import com.wutsi.membership.access.dto.Account
import com.wutsi.platform.core.messaging.Message
import com.wutsi.platform.core.messaging.MessagingType
import com.wutsi.platform.core.messaging.Party
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.regulation.RegulationEngine
import com.wutsi.workflow.WorkflowContext
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale

@Service
class SendOrderToMerchantWorkflow(
    eventStream: EventStream,
    private val mapper: Mapper,
    private val templateEngine: TemplateEngine,
    private val regulationEngine: RegulationEngine,
    private val mailFilterSet: MailFilterSet,
) : AbstractSendOrderWorkflow(eventStream) {
    override fun createMessage(
        order: Order,
        merchant: Account,
        type: MessagingType,
        context: WorkflowContext,
    ): Message? =
        when (type) {
            MessagingType.EMAIL -> merchant.email?.let {
                Message(
                    recipient = Party(
                        email = it,
                        displayName = merchant.displayName,
                    ),
                    subject = getText("email.notify-merchant.subject"),
                    body = generateBody(order, merchant),
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

    private fun generateBody(order: Order, merchant: Account): String {
        val ctx = Context(Locale(merchant.language))
        val country = regulationEngine.country(order.business.country)
        ctx.setVariable("order", mapper.toOrderModel(order, country))

        val body = templateEngine.process("order-merchant.html", ctx)
        return mailFilterSet.filter(
            body = body,
            context = createMailContext(merchant),
        )
    }

    private fun getDeviceToken(merchant: Account): String? =
        try {
            membershipAccessApi.getAccountDevice(merchant.id).device.token
        } catch (ex: Exception) {
            null
        }
}
