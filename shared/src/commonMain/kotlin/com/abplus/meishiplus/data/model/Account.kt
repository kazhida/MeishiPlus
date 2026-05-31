package com.abplus.meishiplus.data.model

sealed class Account(
    open val service: String,
    open val userId: String,
    open val userUrl: String,
) {
    data class Facebook(
        override val service: String,
        override val userId: String,
        override val userUrl: String
    ) : Account(service, userId, userUrl)

    data class X(
        override val service: String,
        override val userId: String,
        override val userUrl: String
    ) : Account(service, userId, userUrl)

    data class Google(
        override val service: String,
        override val userId: String,
    ) : Account(service, userId, "")

    data class Github(
        override val service: String,
        override val userId: String,
        override val userUrl: String
    ) : Account(service, userId, userUrl)

    data class Qiita(
        override val service: String,
        override val userId: String,
        override val userUrl: String = ""
    ) : Account(service, userId, userUrl)

    data class Instagram(
        override val service: String,
        override val userId: String,
        override val userUrl: String = ""
    ) : Account(service, userId, userUrl)
}
