package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.instagram_client_id
import meishiplus.shared.generated.resources.instagram_client_secret
import org.jetbrains.compose.resources.getString

object InstagramAuth {

    suspend fun authorizationUrl(
        clientId: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        scope: String = DEFAULT_SCOPE,
        state: String = "",
    ): String {
        return URLBuilder(AUTHORIZATION_URL).apply {
            parameters.append("client_id", clientId ?: defaultClientId())
            parameters.append("redirect_uri", redirectUri)
            parameters.append("scope", scope)
            parameters.append("response_type", "code")
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
        httpClient: HttpClient = defaultHttpClient,
    ): Account.Instagram {
        val accessToken = exchangeCodeForAccessToken(
            code = code,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
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
        httpClient: HttpClient = defaultHttpClient,
    ): String {
        require(code.isNotBlank()) { "code must not be blank." }

        val response = httpClient.post(TOKEN_URL) {
            accept(ContentType.Application.Json)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", clientId ?: defaultClientId())
                        append("client_secret", clientSecret ?: defaultClientSecret())
                        append("grant_type", "authorization_code")
                        append("redirect_uri", redirectUri)
                        append("code", code)
                    },
                ),
            )
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("Instagram authentication failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        return response.body<AccessTokenResponse>().accessToken
    }

    suspend fun getAuthenticatedAccount(
        accessToken: String,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.Instagram {
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }

        val url = URLBuilder("$GRAPH_API_BASE_URL/me").apply {
            parameters.append("fields", "id,username,user_id")
            parameters.append("access_token", accessToken)
        }.buildString()
        val response = httpClient.get(url) {
            accept(ContentType.Application.Json)
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("Instagram authenticated user request failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        val user = response.body<AuthenticatedUserResponse>()
        val userId = user.username ?: user.userId ?: user.id
        return Account.Instagram(
            service = SERVICE,
            userId = userId,
            userUrl = user.username?.let { "$INSTAGRAM_WEB_BASE_URL/$it" }.orEmpty(),
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
        getString(Res.string.instagram_client_id)

    private suspend fun defaultClientSecret(): String =
        getString(Res.string.instagram_client_secret)

    @Serializable
    private data class AccessTokenResponse(
        @SerialName("access_token")
        val accessToken: String,
    )

    @Serializable
    private data class AuthenticatedUserResponse(
        val id: String,
        val username: String? = null,
        @SerialName("user_id")
        val userId: String? = null,
    )

    private const val AUTHORIZATION_URL = "https://api.instagram.com/oauth/authorize"
    private const val TOKEN_URL = "https://api.instagram.com/oauth/access_token"
    private const val GRAPH_API_BASE_URL = "https://graph.instagram.com"
    private const val INSTAGRAM_WEB_BASE_URL = "https://www.instagram.com"
    private const val SERVICE = "instagram"
    private const val DEFAULT_SCOPE = "instagram_business_basic"
    const val DEFAULT_REDIRECT_URI = "mspls://instagram"
}
