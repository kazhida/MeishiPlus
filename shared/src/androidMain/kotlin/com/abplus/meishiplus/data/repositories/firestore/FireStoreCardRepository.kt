package com.abplus.meishiplus.data.repositories.firestore

import com.abplus.meishiplus.data.entities.CardEntity
import com.abplus.meishiplus.data.model.Account
import com.abplus.meishiplus.data.repositories.CardRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FireStoreCardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CardRepository {
    private val cards = firestore.collection(CARDS_COLLECTION)

    override suspend fun addCard(card: CardEntity): CardEntity {
        val document = cards.document()
        val cardWithId = card.withInitializedLayout().copy(id = document.id)
        document
            .set(cardWithId.toMap())
            .await()
        return cardWithId
    }

    override suspend fun getCard(id: String): CardEntity =
        cards.document(id)
            .get()
            .await()
            .toCardEntity()
            ?.withInitializedLayout()
            ?: error("Card not found: $id")

    override suspend fun getCards(cardIds: List<String>): List<CardEntity> {
        if (cardIds.isEmpty()) return emptyList()

        return cardIds.map { id ->
            getCard(id)
        }
    }

    override suspend fun saveCard(card: CardEntity) {
        cards.document(card.id)
            .set(card.withInitializedLayout().toMap())
            .await()
    }

    override suspend fun deleteCard(id: String) {
        cards.document(id)
            .delete()
            .await()
    }

    override suspend fun updateCard(card: CardEntity) {
        saveCard(card)
    }

    override suspend fun appendPartnerId(cardId: String, partnerCardId: String) {
        cards.document(cardId)
            .update(
                "partnerIds",
                FieldValue.arrayUnion(partnerCardId),
            )
            .await()
    }

    private companion object {
        const val CARDS_COLLECTION = "cards"
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCardEntity(): CardEntity? {
    val data = data ?: return null
    return CardEntity(
        id = data["id"] as? String ?: id,
        ownerUid = data["ownerUid"] as? String ?: "",
        caption = data["caption"] as? String ?: "----",
        name = data["name"].toCardElement(default = CardEntity.default().name),
        email = data["email"].toCardElement(default = CardEntity.default().email),
        address1 = data["address1"].toCardElement(default = CardEntity.default().address1),
        address2 = data["address2"].toCardElement(default = CardEntity.default().address2),
        phone = data["phone"].toCardElement(default = CardEntity.default().phone),
        organization = data["organization"].toCardElement(default = CardEntity.default().organization),
        title = data["title"].toCardElement(default = CardEntity.default().title),
        bgAlpha = (data["bgAlpha"] as? Number)?.toFloat() ?: 0f,
        bgFile = data["bgFile"] as? String ?: "",
        createdAt = data["createdAt"].asLong(),
        updatedAt = data["updatedAt"].asLong(),
        remark = data["remark"] as? String ?: "",
        accounts = (data["accounts"] as? List<*>)
            .orEmpty()
            .mapNotNull { it as? Map<*, *> }
            .mapNotNull { it.toAccount() },
        partnerIds = (data["partnerIds"] as? List<*>)
            .orEmpty()
            .mapNotNull { it as? String },
    )
}

private fun CardEntity.toMap(): Map<String, Any?> =
    buildMap {
        put("id", id)
        put("ownerUid", ownerUid)
        put("caption", caption)
        put("name", name.toMap())
        put("email", email.toMap())
        put("address1", address1.toMap())
        put("address2", address2.toMap())
        put("phone", phone.toMap())
        put("organization", organization.toMap())
        put("title", title.toMap())
        if (bgAlpha != 0f) {
            put("bgAlpha", bgAlpha)
        }
        if (bgFile.isNotBlank()) {
            put("bgFile", bgFile)
        }
        if (createdAt != 0L) {
            put("createdAt", createdAt)
        }
        if (updatedAt != 0L) {
            put("updatedAt", updatedAt)
        }
        if (remark.isNotBlank()) {
            put("remark", remark)
        }
        if (accounts.isNotEmpty()) {
            put("accounts", accounts.map { it.toMap() })
        }
        if (partnerIds.isNotEmpty()) {
            put("partnerIds", partnerIds)
        }
    }

private fun CardEntity.CardElement.toMap(): Map<String, Any?> =
    buildMap {
        put("value", value)
        put("x", x)
        put("y", y)
        put("rotation", rotation)
        put("sanserif", sanserif)
        put("fontSize", fontSize)
    }

private fun Any?.toCardElement(default: CardEntity.CardElement): CardEntity.CardElement {
    val map = this as? Map<*, *> ?: return default
    return CardEntity.CardElement(
        value = map["value"] as? String ?: default.value,
        x = (map["x"] as? Number)?.toFloat() ?: default.x,
        y = (map["y"] as? Number)?.toFloat() ?: default.y,
        rotation = (map["rotation"] as? Number)?.toInt() ?: default.rotation,
        sanserif = map["sanserif"] as? Boolean ?: default.sanserif,
        fontSize = (map["fontSize"] as? Number)?.toFloat() ?: default.fontSize,
    )
}

private fun Account.toMap(): Map<String, Any?> =
    buildMap {
        put("service", service)
        put("userId", userName)
        put("userUrl", userUrl)
        if (this@toMap is Account.X && displayName != null) {
            put("displayName", displayName)
        }
    }

private fun Map<*, *>.toAccount(): Account? {
    val service = this["service"] as? String ?: return null
    val userId = this["userId"] as? String ?: return null
    val userUrl = this["userUrl"] as? String ?: ""
    val displayName = this["displayName"] as? String

    return when (service.lowercase()) {
        "facebook" -> Account.Facebook(service, userId, userUrl)
        "x", "twitter" -> Account.X(service, userId, userUrl, displayName)
        "google" -> Account.Google(service, userId)
        "github" -> Account.Github(service, userId, userUrl)
        "instagram" -> Account.Instagram(service, userId, userUrl)
        "qiita" -> Account.Qiita(service, userId, userUrl)
        else -> null
    }
}

private fun Any?.asLong(): Long =
    when (this) {
        is Long -> this
        is Int -> toLong()
        is Double -> toLong()
        is Number -> toLong()
        else -> 0L
    }
