package com.abplus.meishiplus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform