package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
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
import meishiplus.shared.generated.resources.github_client_id
import meishiplus.shared.generated.resources.github_client_secret
import org.jetbrains.compose.resources.getString

object GithubAuth {

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
    ): Account.Github {
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

        val response = httpClient.post("$GITHUB_WEB_BASE_URL/login/oauth/access_token") {
            accept(ContentType.Application.Json)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", clientId ?: defaultClientId())
                        append("client_secret", clientSecret ?: defaultClientSecret())
                        append("code", code)
                        append("redirect_uri", redirectUri)
                    },
                ),
            )
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("GitHub authentication failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        return response.body<AccessTokenResponse>().accessToken
    }

    suspend fun getAuthenticatedAccount(
        accessToken: String,
        httpClient: HttpClient = defaultHttpClient,
    ): Account.Github {
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }

        val response = httpClient.get("$BASE_URL/user") {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        if (response.status.value !in 200..299) {
            val body = response.bodyAsText()
            error("GitHub authenticated user request failed: ${response.status.value} ${response.status.description}. $body".trim())
        }

        val user = response.body<AuthenticatedUserResponse>()
        return Account.Github(
            service = SERVICE,
            userId = user.login,
            userUrl = user.htmlUrl ?: "$GITHUB_WEB_BASE_URL/${user.login}",
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
        getString(Res.string.github_client_id)

    private suspend fun defaultClientSecret(): String =
        getString(Res.string.github_client_secret)

    @Serializable
    private data class AccessTokenResponse(
        @SerialName("access_token")
        val accessToken: String,
    )

    @Serializable
    private data class AuthenticatedUserResponse(
        val login: String,
        @SerialName("html_url")
        val htmlUrl: String? = null,
    )

    private const val BASE_URL = "https://api.github.com"
    private const val GITHUB_WEB_BASE_URL = "https://github.com"
    private const val AUTHORIZATION_URL = "$GITHUB_WEB_BASE_URL/login/oauth/authorize"
    private const val SERVICE = "github"
    private const val DEFAULT_SCOPE = "read:user"
    const val DEFAULT_REDIRECT_URI = "mspls://github"
}
