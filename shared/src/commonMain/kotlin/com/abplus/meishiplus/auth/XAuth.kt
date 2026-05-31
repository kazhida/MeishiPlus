package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.x_client_id
import meishiplus.shared.generated.resources.x_client_secret
import org.jetbrains.compose.resources.getString
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object XAuth {

    suspend fun authorizationUrl(
        clientId: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        codeChallenge: String = DEFAULT_CODE_VERIFIER,
        scope: String = DEFAULT_SCOPE,
        state: String = "",
        codeChallengeMethod: String = CODE_CHALLENGE_METHOD_PLAIN,
    ): String {
        require(redirectUri.isNotBlank()) { "redirectUri must not be blank." }
        require(codeChallenge.isNotBlank()) { "codeChallenge must not be blank." }
        require(codeChallenge.length in PKCE_LENGTH_RANGE) { "codeChallenge must be 43 to 128 characters." }

        return URLBuilder(AUTHORIZATION_URL).apply {
            parameters.append("response_type", "code")
            parameters.append("client_id", clientId ?: defaultClientId())
            parameters.append("redirect_uri", redirectUri)
            parameters.append("scope", scope)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", codeChallengeMethod)
            if (state.isNotBlank()) {
                parameters.append("state", state)
            }
        }.buildString()
    }

    suspend fun authenticate(
        code: String,
        clientId: String? = null,
        clientSecret: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        codeVerifier: String = DEFAULT_CODE_VERIFIER,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.X {
        val accessToken = exchangeCodeForAccessToken(
            code = code,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            codeVerifier = codeVerifier,
            httpClient = httpClient,
        )
        return getAuthenticatedAccount(
            accessToken = accessToken,
            httpClient = httpClient,
        )
    }

    suspend fun exchangeCodeForAccessToken(
        code: String,
        clientId: String? = null,
        clientSecret: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        codeVerifier: String = DEFAULT_CODE_VERIFIER,
        httpClient: HttpClient = defaultHttpClient,
    ): String {
        require(code.isNotBlank()) { "code must not be blank." }
        require(redirectUri.isNotBlank()) { "redirectUri must not be blank." }
        require(codeVerifier.isNotBlank()) { "codeVerifier must not be blank." }
        require(codeVerifier.length in PKCE_LENGTH_RANGE) { "codeVerifier must be 43 to 128 characters." }

        val resolvedClientId = clientId ?: defaultClientId()
        val resolvedClientSecret = clientSecret ?: defaultClientSecret()
        val response = httpClient.post(TOKEN_URL) {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, basicAuthHeader(resolvedClientId, resolvedClientSecret))
            }
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("code", code)
                        append("grant_type", "authorization_code")
                        append("redirect_uri", redirectUri)
                        append("code_verifier", codeVerifier)
                    },
                ),
            )
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("X authentication failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        return response.body<AccessTokenResponse>().accessToken
    }

    suspend fun getAuthenticatedAccount(
        accessToken: String,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.X {
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }

        val response = httpClient.get("$BASE_URL/users/me") {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("X authentication failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        val user = response.body<AuthenticatedUserResponse>().data
        return Account.X(
            service = SERVICE,
            userId = user.id,
            userUrl = user.username
                ?.takeIf { it.isNotBlank() }
                ?.let { "$X_WEB_BASE_URL/$it" }
                ?: "$X_WEB_BASE_URL/i/user/${user.id}",
        )
    }

    private val defaultHttpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    private suspend fun defaultClientId(): String =
        getString(Res.string.x_client_id)

    private suspend fun defaultClientSecret(): String =
        getString(Res.string.x_client_secret)

    @OptIn(ExperimentalEncodingApi::class)
    private fun basicAuthHeader(clientId: String, clientSecret: String): String {
        val credentials = Base64.encode("$clientId:$clientSecret".encodeToByteArray())
        return "Basic $credentials"
    }

    @Serializable
    private data class AccessTokenResponse(
        @SerialName("access_token")
        val accessToken: String,
    )

    @Serializable
    private data class AuthenticatedUserResponse(
        val data: AuthenticatedUser,
    )

    @Serializable
    private data class AuthenticatedUser(
        val id: String,
        val username: String? = null,
    )

    private const val BASE_URL = "https://api.x.com/2"
    private const val AUTHORIZATION_URL = "https://x.com/i/oauth2/authorize"
    private const val TOKEN_URL = "$BASE_URL/oauth2/token"
    private const val X_WEB_BASE_URL = "https://x.com"
    private const val SERVICE = "x"
    private const val DEFAULT_SCOPE = "users.read"
    private const val DEFAULT_CODE_VERIFIER = "meishiplus-x-oauth-code-verifier-0123456789abcdef"
    private const val CODE_CHALLENGE_METHOD_PLAIN = "plain"
    const val DEFAULT_REDIRECT_URI = "mspls://x"
    private val PKCE_LENGTH_RANGE = 43..128
}
