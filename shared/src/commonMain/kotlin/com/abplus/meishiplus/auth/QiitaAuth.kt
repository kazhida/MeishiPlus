package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.qiita_client_id
import meishiplus.shared.generated.resources.qiita_client_secret
import org.jetbrains.compose.resources.getString

object QiitaAuth {

    suspend fun authorizationUrl(
        clientId: String? = null,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        scope: String = DEFAULT_SCOPE,
        state: String = "",
    ): String {
        return URLBuilder("$BASE_URL/oauth/authorize").apply {
            parameters.append("client_id", clientId ?: defaultClientId())
            parameters.append("redirect_uri", redirectUri)
            parameters.append("scope", scope)
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
    ): Account.Qiita {
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

        val response = httpClient.post("$BASE_URL/access_tokens") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("client_id", clientId ?: defaultClientId())
                    put("client_secret", clientSecret ?: defaultClientSecret())
                    put("code", code)
                    put("redirect_uri", redirectUri)
                },
            )
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("Qiita authentication failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        return response.body<AccessTokenResponse>().token
    }

    suspend fun getAuthenticatedAccount(
        accessToken: String,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.Qiita {
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }

        val response = httpClient.get("$BASE_URL/authenticated_user") {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("Qiita authenticated user request failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        val user = response.body<AuthenticatedUserResponse>()
        return Account.Qiita(
            service = SERVICE,
            userId = user.id,
            userUrl = "$QIITA_WEB_BASE_URL/${user.id}",
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
        getString(Res.string.qiita_client_id)

    private suspend fun defaultClientSecret(): String =
        getString(Res.string.qiita_client_secret)

    @Serializable
    private data class AccessTokenResponse(
        val token: String,
    )

    @Serializable
    private data class AuthenticatedUserResponse(
        val id: String,
        @SerialName("profile_image_url")
        val profileImageUrl: String? = null,
    )

    private const val BASE_URL = "https://qiita.com/api/v2"
    private const val QIITA_WEB_BASE_URL = "https://qiita.com"
    private const val SERVICE = "qiita"
    private const val DEFAULT_SCOPE = "read_qiita"
    const val DEFAULT_REDIRECT_URI = "mspls://qiiita"
}
