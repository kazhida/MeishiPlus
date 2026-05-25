package com.abplus.meishiplus.data.entities

data class CardEntity(
    val id: String = "",
    val ownerUid: String = "",
    val caption: String = "タブ名",
    val name: CardElement = CardElement("氏名"),
    val email: CardElement = CardElement("mail@example.com"),
    val address: CardElement = CardElement("住所"),
    val phone: CardElement = CardElement("電話番号"),
    val organization: CardElement = CardElement("組織"),
    val title: CardElement = CardElement("肩書き"),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val partnerIds: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_ID = "default"

        fun default() = CardEntity(id = DEFAULT_ID)
    }

    data class CardElement(
        val value: String = "",
        val x: Float = 1f,
        val y: Float = 1f,
        val rotation: Int = 0,
        val sanserif: Boolean = false,
        val fontSize: Float = 0.16f
    )
}
