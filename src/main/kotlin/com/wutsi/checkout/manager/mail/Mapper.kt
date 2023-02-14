package com.wutsi.checkout.manager.mail

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.access.dto.PaymentMethodSummary
import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.checkout.access.dto.TransactionSummary
import com.wutsi.checkout.manager.util.NumberUtil
import com.wutsi.enums.ProductType
import com.wutsi.enums.TransactionType
import com.wutsi.marketplace.access.dto.Event
import com.wutsi.marketplace.access.dto.FileSummary
import com.wutsi.marketplace.access.dto.Product
import com.wutsi.marketplace.access.dto.ProductSummary
import com.wutsi.marketplace.access.dto.Store
import com.wutsi.platform.payment.core.Status
import com.wutsi.regulation.Country
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

@Service
class Mapper(
    @Value("\${wutsi.application.webapp-url}") private val webappUrl: String,
    @Value("\${wutsi.application.asset-url}") private val assetUrl: String,
) {
    fun toStoreModel(store: Store) = StoreModel(
        id = store.id,
        cancellationPolicy = CancellationPolicyModel(
            accepted = store.cancellationPolicy.accepted,
            window = store.cancellationPolicy.window,
            message = store.cancellationPolicy.message,
        ),
        returnPolicy = ReturnPolicyModel(
            accepted = store.returnPolicy.accepted,
            contactWindow = store.returnPolicy.contactWindow / 24,
            shipBackWindow = store.returnPolicy.shipBackWindow / 24,
            message = store.returnPolicy.message,
        ),
    )

    fun toOrderModel(order: Order, country: Country): OrderModel {
        val fmt = DecimalFormat(country.monetaryFormat)
        return OrderModel(
            id = order.shortId,
            date = order.created.format(DateTimeFormatter.ofPattern(country.dateFormat)),
            customerEmail = order.customerEmail,
            customerName = order.customerName,
            totalPrice = fmt.format(order.totalPrice),
            totalDiscount = if (order.totalDiscount > 0) fmt.format(order.totalDiscount) else null,
            totalPaid = fmt.format(order.totalPaid),
            balance = fmt.format(order.balance),
            subTotalPrice = fmt.format(order.subTotalPrice),
            items = order.items.map {
                OrderItemModel(
                    productId = it.productId,
                    productType = it.productType,
                    title = it.title,
                    pictureUrl = it.pictureUrl,
                    quantity = it.quantity,
                    unitPrice = fmt.format(it.unitPrice),
                    subTotalPrice = fmt.format(it.subTotalPrice),
                    totalPrice = fmt.format(it.totalPrice),
                )
            },
            payment = findPayment(order)?.let { toTransactionModel(it, country) },
            notes = if (order.notes.isNullOrEmpty()) null else order.notes,
        )
    }

    fun toTransactionModel(tx: TransactionSummary, country: Country): TransactionModel {
        val fmt = DecimalFormat(country.monetaryFormat)
        return TransactionModel(
            id = tx.id,
            type = tx.type,
            amount = fmt.format(tx.amount),
            paymentMethod = toPaymentMethodModel(tx.paymentMethod),
        )
    }

    fun toPaymentMethodModel(payment: PaymentMethodSummary) = PaymentMethodModel(
        number = payment.number,
        maskedNumber = "***" + payment.number.takeLast(4),
        type = payment.type,
        provider = toPaymentProviderModel(payment.provider),
    )

    fun toPaymentProviderModel(provider: PaymentProviderSummary) = PaymentProviderModel(
        id = provider.id,
        code = provider.code,
        name = provider.name,
        logoUrl = provider.logoUrl,
    )

    fun toProduct(product: Product, country: Country) = ProductModel(
        id = product.id,
        title = product.title,
        thumbnailUrl = product.thumbnail?.url,
        type = product.type,
        event = if (product.type == ProductType.EVENT.name) product.event?.let { toEventModel(it, country) } else null,
        files = if (product.type == ProductType.DIGITAL_DOWNLOAD.name) product.files.map { toFileModel(it) } else emptyList(),
    )

    fun toProduct(product: ProductSummary, country: Country) = ProductModel(
        id = product.id,
        title = product.title,
        thumbnailUrl = product.thumbnailUrl,
        type = product.type,
        event = product.event?.let { toEventModel(it, country) },
    )

    fun toEventModel(event: Event, country: Country): EventModel {
        val fmt = DateTimeFormatter.ofPattern(country.dateTimeFormat)
        return EventModel(
            online = event.online,
            meetingId = event.meetingId,
            meetingJoinUrl = event.meetingJoinUrl,
            meetingPassword = event.meetingPassword,
            meetingProviderLogoUrl = event.meetingProvider?.logoUrl,
            starts = event.starts?.format(fmt),
            ends = event.ends?.format(fmt),
        )
    }

    fun toFileModel(file: FileSummary, order: OrderModel? = null, item: OrderItemModel? = null) = FileModel(
        id = file.id,
        name = file.name,
        contentSize = NumberUtil.toHumanReadableByteCountSI(file.contentSize.toLong()),
        downloadUrl = toFileDownloadUrl(file, order, item),
        extensionUrl = toExtensionUrl(file.url),
    )

    private fun toExtensionUrl(url: String): String? {
        val file = URL(url).file
        val i = file.lastIndexOf(".")
        return if (i > 0) {
            "$assetUrl/images/file-types/" + file.substring(i + 1).lowercase() + ".png"
        } else {
            return null
        }
    }

    private fun toFileDownloadUrl(file: FileSummary, order: OrderModel?, item: OrderItemModel?): String =
        if (item != null && order != null) {
            "$webappUrl/download?f=${file.id}&p=${item.productId}&o=${order.id}"
        } else {
            "$webappUrl/download?f=${file.id}"
        }

    private fun findPayment(order: Order): TransactionSummary? =
        order.transactions.find { it.status == Status.SUCCESSFUL.name && it.type == TransactionType.CHARGE.name }
}
