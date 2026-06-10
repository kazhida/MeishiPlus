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
    fun qiitaRedirectUri_matchesAndroidIntentFilter() {
        assertEquals("mspls://qiita", QiitaAuth.DEFAULT_REDIRECT_URI)
    }

    @Test
    fun facebookRedirectUri_trimsTrailingSlashFromRedirectServer() {
        assertEquals(
            "https://example.com/facebook",
            FacebookAuth.buildRedirectUri("https://example.com/"),
        )
    }

    @Test
    fun parseSnsAuthRedirect_extractsFacebookRedirectData() {
        val redirect = parseSnsAuthRedirect(
            "mspls://facebook?code=code-123&state=state-1",
        )

        val success = assertIs<SnsAuthRedirectOutcome.Success>(redirect?.resolve())
        assertEquals("facebook", success.service)
        assertEquals("code-123", success.code)
        assertEquals("state-1", success.state)
    }

    @Test
    fun xPkceChallenge_matchesRfc7636Example() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            createXAuthCodeChallenge(verifier),
        )
    }

    @Test
    fun xAuthorizationUrl_storesVerifierForState() = runTest {
        val state = "test-state"
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expectedChallenge = createXAuthCodeChallenge(verifier)

        val url = XAuth.authorizationUrl(
            clientId = "client-id",
            redirectUri = "mspls://x",
            state = state,
            codeVerifier = verifier,
        )

        assertTrue(url.contains("state=$state"))
        assertTrue(url.contains("code_challenge=$expectedChallenge"))
        assertEquals(verifier, resolveXAuthCodeVerifier(state = state))
    }

    @Test
    fun xAuthorizationUrl_includesRequiredDefaultScopes() = runTest {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"

        val url = XAuth.authorizationUrl(
            clientId = "client-id",
            redirectUri = "mspls://x",
            codeVerifier = verifier,
        )

        assertTrue(url.contains("scope=tweet.read%20users.read"))
    }

    @Test
    fun resolve_returnsMissingService_whenServiceIsBlank() {
        val outcome = SnsAuthRedirect(
            service = null,
            code = "code-1",
            state = null,
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
            state = null,
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
            state = null,
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
            state = "pkce-state-1",
            error = null,
            errorDescription = null,
        ).resolve()

        val success = assertIs<SnsAuthRedirectOutcome.Success>(outcome)
        assertEquals("github", success.service)
        assertEquals("code-1", success.code)
        assertEquals("pkce-state-1", success.state)
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
            authenticateSnsAccount("github", "code-github", authenticator = authenticator),
            authenticateSnsAccount("x", "code-x", state = "state-x", authenticator = authenticator),
            authenticateSnsAccount("qiita", "code-qiita", authenticator = authenticator),
            authenticateSnsAccount("instagram", "code-instagram", authenticator = authenticator),
            authenticateSnsAccount("facebook", "code-facebook", authenticator = authenticator),
        )

        assertTrue(accounts[0] is Account.Github)
        assertTrue(accounts[1] is Account.X)
        assertTrue(accounts[2] is Account.Qiita)
        assertTrue(accounts[3] is Account.Instagram)
        assertTrue(accounts[4] is Account.Facebook)
        assertEquals(
            listOf(
                "github:code-github",
                "x:code-x:state-x",
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
        return Account.Github(service = "github", userName = code, userUrl = "https://github.com/$code")
    }

    override suspend fun authenticateX(code: String, state: String?): Account.X {
        calls += "x:$code:${state.orEmpty()}"
        return Account.X(service = "x", userName = code, userUrl = "https://x.com/$code")
    }

    override suspend fun authenticateQiita(code: String): Account.Qiita {
        calls += "qiita:$code"
        return Account.Qiita(service = "qiita", userName = code, userUrl = "https://qiita.com/$code")
    }

    override suspend fun authenticateInstagram(code: String): Account.Instagram {
        calls += "instagram:$code"
        return Account.Instagram(service = "instagram", userName = code, userUrl = "https://instagram.com/$code")
    }

    override suspend fun authenticateFacebook(code: String): Account.Facebook {
        calls += "facebook:$code"
        return Account.Facebook(service = "facebook", userName = code, userUrl = "https://facebook.com/$code")
    }
}
