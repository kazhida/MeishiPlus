package com.abplus.meishiplus.auth

import com.abplus.meishiplus.data.model.Account
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class SnsAuthFlowTest {

    @Test
    fun resolveSnsAuthService_prefersHostOverPath() {
        assertEquals(
            "github",
            resolveSnsAuthService(
                host = "GitHub",
                pathSegments = listOf("x"),
            ),
        )
    }

    @Test
    fun resolveSnsAuthService_fallsBackToPathSegment() {
        assertEquals(
            "qiita",
            resolveSnsAuthService(
                host = null,
                pathSegments = listOf("Qiita", "callback"),
            ),
        )
    }

    @Test
    fun resolveSnsAuthService_returnsNull_whenNoServiceExists() {
        assertEquals(
            null,
            resolveSnsAuthService(
                host = null,
                pathSegments = emptyList(),
            ),
        )
    }

    @Test
    fun resolve_returnsMissingService_whenServiceIsBlank() {
        val outcome = SnsAuthRedirect(
            service = null,
            code = "code-1",
            error = null,
            errorDescription = null,
        ).resolve()

        assertIs<SnsAuthRedirectOutcome.MissingService>(outcome)
    }

    @Test
    fun resolve_returnsFailure_whenErrorIsPresent() {
        val outcome = SnsAuthRedirect(
            service = "github",
            code = "code-1",
            error = "access_denied",
            errorDescription = "user cancelled",
        ).resolve()

        val failure = assertIs<SnsAuthRedirectOutcome.Failure>(outcome)
        assertEquals("access_denied", failure.error)
        assertEquals("user cancelled", failure.description)
    }

    @Test
    fun resolve_returnsMissingCode_whenCodeIsBlank() {
        val outcome = SnsAuthRedirect(
            service = "github",
            code = null,
            error = null,
            errorDescription = null,
        ).resolve()

        assertIs<SnsAuthRedirectOutcome.MissingCode>(outcome)
    }

    @Test
    fun resolve_returnsSuccess_whenServiceAndCodeExist() {
        val outcome = SnsAuthRedirect(
            service = "github",
            code = "code-1",
            error = null,
            errorDescription = null,
        ).resolve()

        val success = assertIs<SnsAuthRedirectOutcome.Success>(outcome)
        assertEquals("github", success.service)
        assertEquals("code-1", success.code)
    }

    @Test
    fun authenticateSnsAccount_dispatchesToGithub() = runTest {
        val authenticator = RecordingAuthenticator()

        val account = authenticateSnsAccount(
            service = "github",
            code = "code-1",
            authenticator = authenticator,
        )

        assertIs<Account.Github>(account)
        assertEquals("github:code-1", authenticator.calls.single())
    }

    @Test
    fun authenticateSnsAccount_dispatchesToEveryService() = runTest {
        val authenticator = RecordingAuthenticator()

        val accounts = listOf(
            authenticateSnsAccount("github", "code-github", authenticator),
            authenticateSnsAccount("x", "code-x", authenticator),
            authenticateSnsAccount("qiita", "code-qiita", authenticator),
            authenticateSnsAccount("instagram", "code-instagram", authenticator),
            authenticateSnsAccount("facebook", "code-facebook", authenticator),
        )

        assertTrue(accounts[0] is Account.Github)
        assertTrue(accounts[1] is Account.X)
        assertTrue(accounts[2] is Account.Qiita)
        assertTrue(accounts[3] is Account.Instagram)
        assertTrue(accounts[4] is Account.Facebook)
        assertEquals(
            listOf(
                "github:code-github",
                "x:code-x",
                "qiita:code-qiita",
                "instagram:code-instagram",
                "facebook:code-facebook",
            ),
            authenticator.calls,
        )
    }

    @Test
    fun authenticateSnsAccount_rejectsUnknownService() {
        runTest {
            val error = assertFailsWith<IllegalStateException> {
                authenticateSnsAccount(
                    service = "unknown",
                    code = "code-1",
                    authenticator = RecordingAuthenticator(),
                )
            }
            assertEquals("未対応のSNSサービスです: unknown", error.message)
        }
    }
}

private class RecordingAuthenticator : SnsAccountAuthenticator {
    val calls = mutableListOf<String>()

    override suspend fun authenticateGithub(code: String): Account.Github {
        calls += "github:$code"
        return Account.Github(service = "github", userId = code, userUrl = "https://github.com/$code")
    }

    override suspend fun authenticateX(code: String): Account.X {
        calls += "x:$code"
        return Account.X(service = "x", userId = code, userUrl = "https://x.com/$code")
    }

    override suspend fun authenticateQiita(code: String): Account.Qiita {
        calls += "qiita:$code"
        return Account.Qiita(service = "qiita", userId = code, userUrl = "https://qiita.com/$code")
    }

    override suspend fun authenticateInstagram(code: String): Account.Instagram {
        calls += "instagram:$code"
        return Account.Instagram(service = "instagram", userId = code, userUrl = "https://instagram.com/$code")
    }

    override suspend fun authenticateFacebook(code: String): Account.Facebook {
        calls += "facebook:$code"
        return Account.Facebook(service = "facebook", userId = code, userUrl = "https://facebook.com/$code")
    }
}
