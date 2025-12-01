package com.example.smd_fyp.model

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    var isRead: Boolean,
    val type: NotificationType
)

