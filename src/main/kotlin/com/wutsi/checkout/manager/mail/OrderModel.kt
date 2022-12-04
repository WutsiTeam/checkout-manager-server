package com.wutsi.checkout.manager.mail

data class OrderModel(
    val id: String,
    val customerName: String,
    val customerEmail: String,
    val subTotalPrice: String? = null,
    val totalPrice: String,
    val totalPaid: String,
    val balance: String,
    val totalDiscount: String?,
    val items: List<OrderItemModel>,
    val date: String
)
