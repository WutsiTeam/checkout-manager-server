package com.wutsi.checkout.manager.mail

import com.wutsi.checkout.access.dto.Order
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
            }
        )
    }
}
