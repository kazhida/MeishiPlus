package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.facebook_client_id
import meishiplus.shared.generated.resources.facebook_client_secret
import org.jetbrains.compose.resources.getString

object FacebookAuth {

    suspend fun authorizationUrl(
        clientId: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        scope: String = DEFAULT_SCOPE,
        state: String = "",
    ): String {
        return URLBuilder("$FACEBOOK_WEB_BASE_URL/$GRAPH_API_VERSION/dialog/oauth").apply {
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
    ): Account.Facebook {
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

        val url = URLBuilder("$GRAPH_API_BASE_URL/$GRAPH_API_VERSION/oauth/access_token").apply {
            parameters.append("client_id", clientId ?: defaultClientId())
            parameters.append("client_secret", clientSecret ?: defaultClientSecret())
            parameters.append("redirect_uri", redirectUri)
            parameters.append("code", code)
        }.buildString()
        val response = httpClient.get(url) {
            accept(ContentType.Application.Json)
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("Facebook authentication failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        return response.body<AccessTokenResponse>().accessToken
    }

    suspend fun getAuthenticatedAccount(
        accessToken: String,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.Facebook {
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }

        val url = URLBuilder("$GRAPH_API_BASE_URL/$GRAPH_API_VERSION/me").apply {
            parameters.append("fields", "id,name")
            parameters.append("access_token", accessToken)
        }.buildString()
        val response = httpClient.get(url) {
            accept(ContentType.Application.Json)
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("Facebook authenticated user request failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        val user = response.body<AuthenticatedUserResponse>()
        return Account.Facebook(
            service = SERVICE,
            userId = user.id,
            userUrl = "$FACEBOOK_WEB_BASE_URL/${user.id}",
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
        getString(Res.string.facebook_client_id)

    private suspend fun defaultClientSecret(): String =
        getString(Res.string.facebook_client_secret)

    @Serializable
    private data class AccessTokenResponse(
        @SerialName("access_token")
        val accessToken: String,
    )

    @Serializable
    private data class AuthenticatedUserResponse(
        val id: String,
        val name: String? = null,
    )

    private const val GRAPH_API_VERSION = "v23.0"
    private const val GRAPH_API_BASE_URL = "https://graph.facebook.com"
    private const val FACEBOOK_WEB_BASE_URL = "https://www.facebook.com"
    private const val SERVICE = "facebook"
    private const val DEFAULT_SCOPE = "public_profile"
    const val DEFAULT_REDIRECT_URI = "mspls://facebook"
}
