package com.wutsi.checkout.manager.mail

data class ProductModel(
    val id: Long,
    val title: String,
    val thumbnailUrl: String? = null,
    val type: String,
    val event: EventModel? = null,
    val files: List<FileModel> = emptyList(),
)
