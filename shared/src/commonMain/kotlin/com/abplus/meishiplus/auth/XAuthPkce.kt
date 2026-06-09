package com.abplus.meishiplus.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal data class XAuthPkceSession(
    val state: String,
    val codeVerifier: String,
)

internal val XAuthPkceLengthRange = 43..128

internal object XAuthPkceSessionStore {
    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, String>()

    suspend fun createAndStore(
        state: String = generateXAuthState(),
        codeVerifier: String = generateXAuthCodeVerifier(),
    ): XAuthPkceSession {
        val session = XAuthPkceSession(
            state = state,
            codeVerifier = codeVerifier,
        )
        require(session.state.isNotBlank()) { "state must not be blank." }
        require(session.codeVerifier.length in XAuthPkceLengthRange) {
            "codeVerifier must be 43 to 128 characters."
        }

        mutex.withLock {
            sessions[session.state] = session.codeVerifier
        }
        return session
    }

    suspend fun consumeCodeVerifier(state: String): String? {
        require(state.isNotBlank()) { "state must not be blank." }

        return mutex.withLock {
            sessions.remove(state)
        }
    }
}

internal suspend fun createXAuthPkceSession(
    state: String? = null,
    codeVerifier: String? = null,
): XAuthPkceSession =
    XAuthPkceSessionStore.createAndStore(
        state = state ?: generateXAuthState(),
        codeVerifier = codeVerifier ?: generateXAuthCodeVerifier(),
    )

internal suspend fun resolveXAuthCodeVerifier(
    state: String?,
    codeVerifier: String? = null,
): String {
    if (!codeVerifier.isNullOrBlank()) {
        require(codeVerifier.length in XAuthPkceLengthRange) {
            "codeVerifier must be 43 to 128 characters."
        }
        return codeVerifier
    }

    require(!state.isNullOrBlank()) {
        "state must not be blank when codeVerifier is not provided."
    }

    return XAuthPkceSessionStore.consumeCodeVerifier(state)
        ?: error("X PKCE session not found for state: $state")
}

internal fun createXAuthCodeChallenge(codeVerifier: String): String {
    require(codeVerifier.isNotBlank()) { "codeVerifier must not be blank." }
    require(codeVerifier.length in XAuthPkceLengthRange) {
        "codeVerifier must be 43 to 128 characters."
    }

    return sha256(codeVerifier.encodeToByteArray()).toBase64UrlWithoutPadding()
}

internal fun generateXAuthCodeVerifier(): String =
    generateXAuthPkceToken(32)

internal fun generateXAuthState(): String =
    generateXAuthPkceToken(16)

private fun generateXAuthPkceToken(byteCount: Int): String {
    require(byteCount > 0) { "byteCount must be positive." }

    return secureRandomBytes(byteCount).toBase64UrlWithoutPadding()
}

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toBase64UrlWithoutPadding(): String =
    Base64.encode(this)
        .replace('+', '-')
        .replace('/', '_')
        .trimEnd('=')

private fun sha256(message: ByteArray): ByteArray {
    val bitLength = message.size.toLong() * 8L
    val blockCount = ((message.size + 9 + 63) / 64).coerceAtLeast(1)
    val padded = ByteArray(blockCount * 64)
    message.copyInto(padded)
    padded[message.size] = 0x80.toByte()

    val lengthIndex = padded.lastIndex
    for (shift in 0 until 8) {
        padded[lengthIndex - shift] = ((bitLength ushr (shift * 8)) and 0xFF).toByte()
    }

    var h0 = 0x6a09e667
    var h1 = 0xbb67ae85.toInt()
    var h2 = 0x3c6ef372
    var h3 = 0xa54ff53a.toInt()
    var h4 = 0x510e527f
    var h5 = 0x9b05688c.toInt()
    var h6 = 0x1f83d9ab
    var h7 = 0x5be0cd19

    val w = IntArray(64)
    var offset = 0
    while (offset < padded.size) {
        for (i in 0 until 16) {
            val index = offset + i * 4
            w[i] = ((padded[index].toInt() and 0xFF) shl 24) or
                ((padded[index + 1].toInt() and 0xFF) shl 16) or
                ((padded[index + 2].toInt() and 0xFF) shl 8) or
                (padded[index + 3].toInt() and 0xFF)
        }

        for (i in 16 until 64) {
            val s0 = smallSigma0(w[i - 15])
            val s1 = smallSigma1(w[i - 2])
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7

        for (i in 0 until 64) {
            val t1 = h + bigSigma1(e) + ch(e, f, g) + SHA256_K[i] + w[i]
            val t2 = bigSigma0(a) + maj(a, b, c)
            h = g
            g = f
            f = e
            e = d + t1
            d = c
            c = b
            b = a
            a = t1 + t2
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        h5 += f
        h6 += g
        h7 += h

        offset += 64
    }

    val output = ByteArray(32)
    writeIntBigEndian(output, 0, h0)
    writeIntBigEndian(output, 4, h1)
    writeIntBigEndian(output, 8, h2)
    writeIntBigEndian(output, 12, h3)
    writeIntBigEndian(output, 16, h4)
    writeIntBigEndian(output, 20, h5)
    writeIntBigEndian(output, 24, h6)
    writeIntBigEndian(output, 28, h7)
    return output
}

private fun writeIntBigEndian(destination: ByteArray, offset: Int, value: Int) {
    destination[offset] = (value ushr 24).toByte()
    destination[offset + 1] = (value ushr 16).toByte()
    destination[offset + 2] = (value ushr 8).toByte()
    destination[offset + 3] = value.toByte()
}

private fun ch(x: Int, y: Int, z: Int): Int =
    (x and y) xor (x.inv() and z)

private fun maj(x: Int, y: Int, z: Int): Int =
    (x and y) xor (x and z) xor (y and z)

private fun bigSigma0(x: Int): Int =
    x.rotateRight(2) xor x.rotateRight(13) xor x.rotateRight(22)

private fun bigSigma1(x: Int): Int =
    x.rotateRight(6) xor x.rotateRight(11) xor x.rotateRight(25)

private fun smallSigma0(x: Int): Int =
    x.rotateRight(7) xor x.rotateRight(18) xor (x ushr 3)

private fun smallSigma1(x: Int): Int =
    x.rotateRight(17) xor x.rotateRight(19) xor (x ushr 10)

private fun Int.rotateRight(bits: Int): Int =
    (this ushr bits) or (this shl (32 - bits))

private val SHA256_K = intArrayOf(
    0x428a2f98,
    0x71374491,
    0xb5c0fbcf.toInt(),
    0xe9b5dba5.toInt(),
    0x3956c25b,
    0x59f111f1,
    0x923f82a4.toInt(),
    0xab1c5ed5.toInt(),
    0xd807aa98.toInt(),
    0x12835b01,
    0x243185be,
    0x550c7dc3,
    0x72be5d74,
    0x80deb1fe.toInt(),
    0x9bdc06a7.toInt(),
    0xc19bf174.toInt(),
    0xe49b69c1.toInt(),
    0xefbe4786.toInt(),
    0x0fc19dc6,
    0x240ca1cc,
    0x2de92c6f,
    0x4a7484aa,
    0x5cb0a9dc,
    0x76f988da,
    0x983e5152.toInt(),
    0xa831c66d.toInt(),
    0xb00327c8.toInt(),
    0xbf597fc7.toInt(),
    0xc6e00bf3.toInt(),
    0xd5a79147.toInt(),
    0x06ca6351,
    0x14292967,
    0x27b70a85,
    0x2e1b2138,
    0x4d2c6dfc,
    0x53380d13,
    0x650a7354,
    0x766a0abb,
    0x81c2c92e.toInt(),
    0x92722c85.toInt(),
    0xa2bfe8a1.toInt(),
    0xa81a664b.toInt(),
    0xc24b8b70.toInt(),
    0xc76c51a3.toInt(),
    0xd192e819.toInt(),
    0xd6990624.toInt(),
    0xf40e3585.toInt(),
    0x106aa070,
    0x19a4c116,
    0x1e376c08,
    0x2748774c,
    0x34b0bcb5,
    0x391c0cb3,
    0x4ed8aa4a,
    0x5b9cca4f,
    0x682e6ff3,
    0x748f82ee,
    0x78a5636f,
    0x84c87814.toInt(),
    0x8cc70208.toInt(),
    0x90befffa.toInt(),
    0xa4506ceb.toInt(),
    0xbef9a3f7.toInt(),
    0xc67178f2.toInt(),
)

internal expect fun secureRandomBytes(length: Int): ByteArray
