package com.wutsi.checkout.manager.mail

data class OrderModel(
    val id: String,
    val customerName: String,
    val customerEmail: String,
    val subTotalPrice: String?,
    val totalPrice: String,
    val totalPaid: String,
    val balance: String,
    val totalDiscount: String?,
    val items: List<OrderItemModel>,
    val date: String,
    val payment: TransactionModel?,
) {
    val itemsWithEvent: List<OrderItemModel>
        get() = items.filter { it.event != null }

    val itemsWithFiles: List<OrderItemModel>
        get() = items.filter { it.files.isNotEmpty() }
}
