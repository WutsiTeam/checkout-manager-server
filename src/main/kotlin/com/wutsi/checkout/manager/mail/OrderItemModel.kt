package com.wutsi.checkout.manager.mail

data class OrderItemModel(
    val productId: Long,
    val productType: String,
    val title: String,
    val pictureUrl: String?,
    val quantity: Int,
    val unitPrice: String,
    val subTotalPrice: String,
    val totalPrice: String,
    var event: EventModel? = null,
    var files: List<FileModel> = emptyList(),
)
