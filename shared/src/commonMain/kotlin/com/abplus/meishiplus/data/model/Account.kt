package com.abplus.meishiplus.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Account(
    open val service: String,
    open val userName: String,
    open val userUrl: String,
) {
    val profileUrl: String
        get() = userUrl.ifBlank {
            when (service.lowercase()) {
                "facebook" -> "https://www.facebook.com/$userName"
                "x", "twitter" -> "https://x.com/i/user/$userName"
                "github" -> "https://github.com/$userName"
                "qiita" -> "https://qiita.com/$userName"
                "instagram" -> "https://www.instagram.com/$userName"
                else -> ""
            }
        }

    data class Facebook(
        override val service: String,
        override val userName: String,
        override val userUrl: String,
    ) : Account(service, userName, userUrl)

    data class X(
        override val service: String,
        override val userName: String,
        override val userUrl: String,
        val displayName: String? = null,
    ) : Account(service, userName, userUrl)

    data class Google(
        override val service: String,
        override val userName: String,
    ) : Account(service, userName, "")

    data class Github(
        override val service: String,
        override val userName: String,
        override val userUrl: String,
    ) : Account(service, userName, userUrl)

    data class Qiita(
        override val service: String,
        override val userName: String,
        override val userUrl: String = "",
    ) : Account(service, userName, userUrl)

    data class Instagram(
        override val service: String,
        override val userName: String,
        override val userUrl: String = "",
    ) : Account(service, userName, userUrl)
}
