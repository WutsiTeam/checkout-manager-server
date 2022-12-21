package com.wutsi.checkout.manager.mail

data class FileModel(
    val id: Long = 0,
    val name: String = "",
    val downloadUrl: String = "",
    val contentSize: String = "",
    val extensionUrl: String? = null,
)
