package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import meishiplus.shared.generated.resources.Res
import meishiplus.shared.generated.resources.sns_auth_failed
import meishiplus.shared.generated.resources.sns_auth_invalid_redirect
import meishiplus.shared.generated.resources.sns_auth_missing_code
import meishiplus.shared.generated.resources.sns_auth_reflect_failed
import meishiplus.shared.generated.resources.sns_auth_unsupported_service
import org.jetbrains.compose.resources.getString

data class SnsAuthRedirect(
    val service: String?,
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?,
) {
    fun resolve(): SnsAuthRedirectOutcome =
        when {
            service.isNullOrBlank() -> SnsAuthRedirectOutcome.MissingService
            !error.isNullOrBlank() -> SnsAuthRedirectOutcome.Failure(
                error = error,
                description = errorDescription?.takeIf { it.isNotBlank() },
            )
            code.isNullOrBlank() -> SnsAuthRedirectOutcome.MissingCode
            else -> SnsAuthRedirectOutcome.Success(
                service = service,
                code = code,
                state = state,
            )
        }
}

object SnsAuthDeepLinkState {
    val pendingUrl = MutableStateFlow<String?>(null)

    fun submit(url: String?) {
        pendingUrl.value = url
    }

    fun clear() {
        pendingUrl.value = null
    }
}

fun resolveSnsAuthService(
    host: String?,
    pathSegments: List<String>,
): String? =
    host?.takeIf { it.isNotBlank() }?.lowercase()
        ?: pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }?.lowercase()

fun parseSnsAuthRedirect(url: String): SnsAuthRedirect? {
    val schemeIndex = url.indexOf("://")
    if (schemeIndex < 0) return null

    val remainder = url.substring(schemeIndex + 3)
    val queryIndex = remainder.indexOf('?')
    val authorityAndPath = if (queryIndex >= 0) {
        remainder.substring(0, queryIndex)
    } else {
        remainder
    }
    val queryString = if (queryIndex >= 0) {
        remainder.substring(queryIndex + 1)
    } else {
        ""
    }

    val authorityParts = authorityAndPath
        .split('/')
        .filter { it.isNotBlank() }
    val service = resolveSnsAuthService(
        host = authorityParts.firstOrNull(),
        pathSegments = authorityParts.drop(1),
    )

    val queryParams = queryString
        .split('&')
        .mapNotNull { entry ->
            val separatorIndex = entry.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null
            entry.substring(0, separatorIndex) to entry.substring(separatorIndex + 1)
        }
        .toMap()

    return SnsAuthRedirect(
        service = service,
        code = queryParams["code"],
        state = queryParams["state"],
        error = queryParams["error"],
        errorDescription = queryParams["error_description"] ?: queryParams["error_reason"],
    )
}

fun List<Account>.upsertAccount(account: Account): List<Account> {
    val service = account.service.lowercase()
    return filterNot { it.service.lowercase() == service } + account
}

suspend fun snsAuthInvalidRedirectMessage(): String =
    getString(Res.string.sns_auth_invalid_redirect)

suspend fun snsAuthMissingCodeMessage(): String =
    getString(Res.string.sns_auth_missing_code)

suspend fun snsAuthFailedMessage(error: String, description: String?): String =
    listOfNotNull(
        getString(Res.string.sns_auth_failed),
        error,
        description?.takeIf { it.isNotBlank() },
    ).joinToString(" ")

suspend fun snsAuthReflectFailedMessage(): String =
    getString(Res.string.sns_auth_reflect_failed)

suspend fun snsAuthUnsupportedServiceMessage(): String =
    getString(Res.string.sns_auth_unsupported_service)

sealed interface SnsAuthRedirectOutcome {
    data object MissingService : SnsAuthRedirectOutcome

    data object MissingCode : SnsAuthRedirectOutcome

    data class Failure(
        val error: String,
        val description: String? = null,
    ) : SnsAuthRedirectOutcome

    data class Success(
        val service: String,
        val code: String,
        val state: String?,
    ) : SnsAuthRedirectOutcome
}

interface SnsAccountAuthenticator {
    suspend fun authenticateGithub(code: String): Account.Github

    suspend fun authenticateX(code: String, state: String? = null): Account.X

    suspend fun authenticateQiita(code: String): Account.Qiita

    suspend fun authenticateInstagram(code: String): Account.Instagram

    suspend fun authenticateFacebook(code: String): Account.Facebook
}

object DefaultSnsAccountAuthenticator : SnsAccountAuthenticator {
    override suspend fun authenticateGithub(code: String): Account.Github =
        GithubAuth.authenticate(code)

    override suspend fun authenticateX(code: String, state: String?): Account.X =
        XAuth.authenticate(code = code, state = state)

    override suspend fun authenticateQiita(code: String): Account.Qiita =
        QiitaAuth.authenticate(code)

    override suspend fun authenticateInstagram(code: String): Account.Instagram =
        InstagramAuth.authenticate(code)

    override suspend fun authenticateFacebook(code: String): Account.Facebook =
        FacebookAuth.authenticate(code)
}

suspend fun authenticateSnsAccount(
    service: String,
    code: String,
    state: String? = null,
    authenticator: SnsAccountAuthenticator = DefaultSnsAccountAuthenticator,
): Account =
    when (service.lowercase()) {
        "github" -> authenticator.authenticateGithub(code)
        "x" -> authenticator.authenticateX(code, state)
        "qiita" -> authenticator.authenticateQiita(code)
        "instagram" -> authenticator.authenticateInstagram(code)
        "facebook" -> authenticator.authenticateFacebook(code)
        else -> error("未対応のSNSサービスです: $service")
    }
