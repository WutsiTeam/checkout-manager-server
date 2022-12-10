package com.wutsi.checkout.manager.mail

import com.wutsi.checkout.access.dto.Order
import com.wutsi.checkout.access.dto.PaymentMethodSummary
import com.wutsi.checkout.access.dto.PaymentProviderSummary
import com.wutsi.checkout.access.dto.TransactionSummary
import com.wutsi.enums.TransactionType
import com.wutsi.platform.payment.core.Status
import com.wutsi.regulation.Country
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

@Service
class MailMapper {
    fun toOrderModel(order: Order, country: Country): OrderModel {
        val fmt = DecimalFormat(country.monetaryFormat)
        return OrderModel(
            id = order.id,
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
                    title = it.title,
                    pictureUrl = it.pictureUrl,
                    quantity = it.quantity,
                    unitPrice = fmt.format(it.unitPrice),
                    subTotalPrice = fmt.format(it.subTotalPrice),
                    totalPrice = fmt.format(it.totalPrice)
                )
            },
            payment = findPayment(order)?.let { toTransactionModel(it, country) }
        )
    }

    fun toTransactionModel(tx: TransactionSummary, country: Country): TransactionModel {
        val fmt = DecimalFormat(country.monetaryFormat)
        return TransactionModel(
            id = tx.id,
            type = tx.type,
            amount = fmt.format(tx.amount),
            paymentMethod = toPaymentMethodModel(tx.paymentMethod)
        )
    }

    fun toPaymentMethodModel(payment: PaymentMethodSummary) = PaymentMethodModel(
        number = payment.number,
        maskedNumber = "***" + payment.number.takeLast(4),
        type = payment.type,
        provider = toPaymentProviderModel(payment.provider)
    )

    fun toPaymentProviderModel(provider: PaymentProviderSummary) = PaymentProviderModel(
        id = provider.id,
        code = provider.code,
        name = provider.name,
        logoUrl = provider.logoUrl
    )

    private fun findPayment(order: Order): TransactionSummary? =
        order.transactions.find { it.status == Status.SUCCESSFUL.name && it.type == TransactionType.CHARGE.name }
}
