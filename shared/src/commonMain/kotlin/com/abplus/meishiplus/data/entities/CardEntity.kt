package com.abplus.meishiplus.data.entities

data class CardEntity(
    val id: String = "",
    val ownerUid: String = "",
    val caption: String = "タブ",
    val name: CardElement = CardElement("日高 重機", x = 0.07f, y = 0.33f, fontSize = 24f),
    val email: CardElement = CardElement("hidaka@example.com", x = 0.20f, y = 0.66f, fontSize = 12f),
    val address1: CardElement = CardElement("〒100-8111 東京都千代田区", x = 0.20f, y = 0.77f, fontSize = 12f),
    val address2: CardElement = CardElement("千代田 1-1", x = 0.20f, y = 0.77f, fontSize = 12f),
    val phone: CardElement = CardElement("+1 234-5678", x = 0.20f, y = 0.55f, fontSize = 12f),
    val organization: CardElement = CardElement("秘密結社", x = 0.07f, y = 0.12f, fontSize = 14f),
    val title: CardElement = CardElement("2等陸佐", x = 0.07f, y = 0.23f, fontSize = 14f),
    val bgAlpha: Float = 0f,
    val bgFile: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val partnerIds: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_ID = "default"

        fun default() = CardEntity(id = DEFAULT_ID)
    }

    fun withInitializedLayout(): CardEntity {
        val default = default()
        return copy(
            name = name.withInitializedLayout(default.name),
            email = email.withInitializedLayout(default.email),
            address1 = address1.withInitializedLayout(default.address1),
            address2 = address2.withInitializedLayout(default.address2),
            phone = phone.withInitializedLayout(default.phone),
            organization = organization.withInitializedLayout(default.organization),
            title = title.withInitializedLayout(default.title),
        )
    }

    data class CardElement(
        val value: String = "",
        var x: Float = 0f,
        var y: Float = 0f,
        var rotation: Int = 0,
        var sanserif: Boolean = false,
        var fontSize: Float = 14f
    )
}

private fun CardEntity.CardElement.withInitializedLayout(
    default: CardEntity.CardElement,
): CardEntity.CardElement {
    if (fontSize >= 1f) return this

    return copy(
        x = default.x,
        y = default.y,
        fontSize = default.fontSize,
    )
}
