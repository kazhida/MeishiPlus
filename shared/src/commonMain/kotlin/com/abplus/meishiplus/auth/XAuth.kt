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
        state: String? = null,
        codeVerifier: String? = null,
        scope: String = DEFAULT_SCOPE,
        codeChallengeMethod: String = CODE_CHALLENGE_METHOD_S256,
    ): String {
        require(redirectUri.isNotBlank()) { "redirectUri must not be blank." }

        val pkceSession = createXAuthPkceSession(
            state = state,
            codeVerifier = codeVerifier,
        )
        val resolvedCodeChallengeMethod = when (codeChallengeMethod.lowercase()) {
            CODE_CHALLENGE_METHOD_S256.lowercase() -> CODE_CHALLENGE_METHOD_S256
            CODE_CHALLENGE_METHOD_PLAIN -> CODE_CHALLENGE_METHOD_PLAIN
            else -> error("Unsupported code challenge method: $codeChallengeMethod")
        }
        val resolvedCodeChallenge = when (resolvedCodeChallengeMethod) {
            CODE_CHALLENGE_METHOD_S256 -> createXAuthCodeChallenge(pkceSession.codeVerifier)
            CODE_CHALLENGE_METHOD_PLAIN -> pkceSession.codeVerifier
            else -> error("Unsupported code challenge method: $resolvedCodeChallengeMethod")
        }

        return URLBuilder(AUTHORIZATION_URL).apply {
            parameters.append("response_type", "code")
            parameters.append("client_id", clientId ?: defaultClientId())
            parameters.append("redirect_uri", redirectUri)
            parameters.append("scope", scope)
            parameters.append("code_challenge", resolvedCodeChallenge)
            parameters.append("code_challenge_method", resolvedCodeChallengeMethod)
            parameters.append("state", pkceSession.state)
        }.buildString()
    }

    suspend fun authenticate(
        code: String,
        clientId: String? = null,
        clientSecret: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null,
        codeVerifier: String? = null,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.X {
        val resolvedCodeVerifier = resolveXAuthCodeVerifier(
            state = state,
            codeVerifier = codeVerifier,
        )
        val accessToken = exchangeCodeForAccessToken(
            code = code,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            codeVerifier = resolvedCodeVerifier,
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
        codeVerifier: String,
        httpClient: HttpClient = defaultHttpClient,
    ): String {
        require(code.isNotBlank()) { "code must not be blank." }
        require(redirectUri.isNotBlank()) { "redirectUri must not be blank." }
        require(codeVerifier.isNotBlank()) { "codeVerifier must not be blank." }
        require(codeVerifier.length in XAuthPkceLengthRange) { "codeVerifier must be 43 to 128 characters." }

        val resolvedClientId = clientId ?: defaultClientId()
        val resolvedClientSecret = clientSecret ?: defaultClientSecret()
        val response = httpClient.post(TOKEN_URL) {
            accept(ContentType.Application.Json)
            if (resolvedClientSecret.isNotBlank()) {
                headers {
                    append(HttpHeaders.Authorization, basicAuthHeader(resolvedClientId, resolvedClientSecret))
                }
            }
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("code", code)
                        append("grant_type", "authorization_code")
                        if (resolvedClientSecret.isBlank()) {
                            append("client_id", resolvedClientId)
                        }
                        append("redirect_uri", redirectUri)
                        append("code_verifier", codeVerifier)
                    },
                ),
            )
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("X token exchange failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        val token = response.body<AccessTokenResponse>()
        if (token.scope?.split(" ").orEmpty().none { it == "users.read" }) {
            error("X access token does not include users.read scope: ${token.scope.orEmpty()}".trim())
        }
        return token.accessToken
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
            error("X authenticated user request failed: ${response.status.value} ${response.status.description}. $body".trim())
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
        @SerialName("token_type")
        val tokenType: String? = null,
        val scope: String? = null,
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
    private const val CODE_CHALLENGE_METHOD_PLAIN = "plain"
    private const val CODE_CHALLENGE_METHOD_S256 = "S256"
    const val DEFAULT_REDIRECT_URI = "mspls://x"
}
