package com.example.ragnarok.securitycammobile.model

data class User(val name: String,
                val email: String,
                val registrationTokens: MutableList<String>) {
    constructor() : this("", "", mutableListOf())
}