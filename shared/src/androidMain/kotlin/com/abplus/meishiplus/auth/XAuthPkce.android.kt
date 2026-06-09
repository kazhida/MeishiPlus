package com.abplus.meishiplus.auth

import java.security.SecureRandom

private val secureRandom = SecureRandom()

actual fun secureRandomBytes(length: Int): ByteArray {
    require(length > 0) { "length must be positive." }
    return ByteArray(length).also { secureRandom.nextBytes(it) }
}
