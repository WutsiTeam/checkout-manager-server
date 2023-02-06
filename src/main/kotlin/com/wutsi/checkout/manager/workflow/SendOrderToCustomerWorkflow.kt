package com.wutsi.checkout.manager.workflow

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.manager.event.InternalEventURN
import com.wutsi.checkout.manager.mail.Mapper
import com.wutsi.checkout.manager.mail.OrderModel
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
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.Locale

@Service
class SendOrderToCustomerWorkflow(
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
            MessagingType.EMAIL -> Message(
                recipient = Party(
                    email = order.customerEmail,
                    displayName = order.customerName,
                ),
                subject = getText("email.notify-customer.subject", arrayOf(merchant.displayName.uppercase())),
                body = generateBody(order, merchant),
                mimeType = "text/html;charset=UTF-8",
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
        val ctx = Context(Locale(merchant.language))
        val country = regulationEngine.country(order.business.country)

        ctx.setVariable("order", toOrderModel(order, country))

        val body = templateEngine.process("order-customer.html", ctx)
        return mailFilterSet.filter(
            body = body,
            context = createMailContext(merchant),
        )
    }

    private fun toOrderModel(order: Order, country: Country): OrderModel {
        val model = mapper.toOrderModel(order, country)
        attachEvents(model, country)
        attachFiles(model)
        return model
    }

    private fun attachEvents(order: OrderModel, country: Country) {
        val productIds = order.items
            .filter { it.productType == ProductType.EVENT.name }
            .map { it.productId }
        if (productIds.isEmpty()) {
            return
        }

        val products = marketplaceAccessApi.searchProduct(
            request = SearchProductRequest(
                productIds = productIds,
                limit = productIds.size,
            ),
        ).products.associateBy { it.id }
        order.items
            .filter { it.productType == ProductType.EVENT.name }
            .forEach {
                it.event = products[it.productId]?.event?.let { mapper.toEventModel(it, country) }
            }
    }

    private fun attachFiles(order: OrderModel) {
        order.items
            .filter { it.productType == ProductType.DIGITAL_DOWNLOAD.name }
            .forEach {
                val product = marketplaceAccessApi.getProduct(it.productId).product
                val item = it
                it.files = product.files.map { mapper.toFileModel(it, order, item) }
            }
    }
}
