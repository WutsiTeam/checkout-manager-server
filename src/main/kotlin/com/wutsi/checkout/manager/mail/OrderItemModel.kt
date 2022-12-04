package com.wutsi.checkout.manager.mail

data class OrderItemModel(
    val productId: Long,
    val title: String,
    val pictureUrl: String?,
    val quantity: Int,
    val unitPrice: String,
    val subTotalPrice: String,
    val totalPrice: String
)
