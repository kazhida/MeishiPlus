package com.abplus.meishiplus.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual fun secureRandomBytes(length: Int): ByteArray {
    require(length > 0) { "length must be positive." }

    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        val status = SecRandomCopyBytes(
            kSecRandomDefault,
            length.convert(),
            pinned.addressOf(0),
        )
        check(status == errSecSuccess) {
            "Failed to generate secure random bytes: $status"
        }
    }
    return bytes
}
