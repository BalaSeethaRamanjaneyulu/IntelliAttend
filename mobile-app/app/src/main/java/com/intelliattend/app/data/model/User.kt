package com.intelliattend.app.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "", // "student" or "faculty"
    val name: String = ""
)
