package com.abplua.meishiplus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform