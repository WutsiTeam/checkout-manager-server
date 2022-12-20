package com.wutsi.checkout.manager.mail

data class EventModel(
    val meetingId: String,
    val meetingPassword: String?,
    val meetingJoinUrl: String?,
    val meetingProviderLogoUrl: String?,
    val online: Boolean,
    val starts: String?,
    val ends: String?,
)
