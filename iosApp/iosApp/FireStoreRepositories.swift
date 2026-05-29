import FirebaseFirestore
import Shared

final class FireStoreCardRepository: CardRepository {
    private let cards = Firestore.firestore().collection("cards")

    func addCard(card: CardEntity) async throws -> CardEntity {
        let document = cards.document()
        let cardWithId = Self.cardEntity(card: card, id: document.documentID)
        try await document.setData(Self.dictionary(card: cardWithId))
        return cardWithId
    }

    func getCard(id: String) async throws -> CardEntity {
        let snapshot = try await cards.document(id).getDocument()
        guard let data = snapshot.data() else {
            throw RepositoryError.documentNotFound("cards/\(id)")
        }
        return Self.cardEntity(id: id, data: data)
    }

    func getCards(cardIds: [String]) async throws -> [CardEntity] {
        guard !cardIds.isEmpty else {
            return []
        }

        return try await withThrowingTaskGroup(of: (Int, CardEntity).self) { group in
            for (index, id) in cardIds.enumerated() {
                group.addTask {
                    (index, try await self.getCard(id: id))
                }
            }

            var indexedCards: [(Int, CardEntity)] = []
            for try await indexedCard in group {
                indexedCards.append(indexedCard)
            }
            return indexedCards
                .sorted { $0.0 < $1.0 }
                .map { $0.1 }
        }
    }

    func saveCard(card: CardEntity) async throws {
        try await cards.document(card.id).setData(Self.dictionary(card: card))
    }

    func updateCard(card: CardEntity) async throws {
        try await saveCard(card: card)
    }

    func deleteCard(id: String) async throws {
        try await cards.document(id).delete()
    }

    private static func dictionary(card: CardEntity) -> [String: Any] {
        [
            "id": card.id,
            "ownerUid": card.ownerUid,
            "caption": card.caption,
            "name": cardElementDictionary(element: card.name),
            "email": cardElementDictionary(element: card.email),
            "address1": cardElementDictionary(element: card.address1),
            "address2": cardElementDictionary(element: card.address2),
            "phone": cardElementDictionary(element: card.phone),
            "organization": cardElementDictionary(element: card.organization),
            "title": cardElementDictionary(element: card.title),
            "bgAlpha": card.bgAlpha,
            "bgFile": card.bgFile,
            "createdAt": card.createdAt,
            "updatedAt": card.updatedAt,
            "partnerIds": card.partnerIds,
        ]
    }

    private static func cardEntity(card: CardEntity, id: String) -> CardEntity {
        CardEntity(
            id: id,
            ownerUid: card.ownerUid,
            caption: card.caption,
            name: card.name,
            email: card.email,
            address1: card.address1,
            address2: card.address2,
            phone: card.phone,
            organization: card.organization,
            title: card.title,
            bgAlpha: card.bgAlpha,
            bgFile: card.bgFile,
            createdAt: card.createdAt,
            updatedAt: card.updatedAt,
            partnerIds: card.partnerIds
        )
    }

    private static func cardEntity(id: String, data: [String: Any]) -> CardEntity {
        CardEntity(
            id: data["id"] as? String ?? id,
            ownerUid: data["ownerUid"] as? String ?? "",
            caption: data["caption"] as? String ?? "",
            name: cardElement(data["name"], defaultValue: "氏名", x: 0.07, y: 0.33, fontSize: 24),
            email: cardElement(data["email"], defaultValue: "mail@example.com", x: 0.20, y: 0.66, fontSize: 12),
            address1: cardElement(data["address1"], defaultValue: "住所", x: 0.20, y: 0.77, fontSize: 12),
            address2: cardElement(data["address2"] ?? data["address"], defaultValue: "", x: 0.20, y: 0.77, fontSize: 12),
            phone: cardElement(data["phone"], defaultValue: "電話番号", x: 0.20, y: 0.55, fontSize: 12),
            organization: cardElement(data["organization"], defaultValue: "組織", x: 0.07, y: 0.12, fontSize: 14),
            title: cardElement(data["title"], defaultValue: "肩書き", x: 0.07, y: 0.23, fontSize: 14),
            bgAlpha: floatValue(data["bgAlpha"]),
            bgFile: data["bgFile"] as? String ?? "",
            createdAt: int64Value(data["createdAt"]),
            updatedAt: int64Value(data["updatedAt"]),
            partnerIds: data["partnerIds"] as? [String] ?? []
        )
    }
}

final class FireStoreUserRepository: UserRepository {
    private let users = Firestore.firestore().collection("users")
    private let cards = Firestore.firestore().collection("cards")

    func addUser(user: UserEntity) async throws -> UserEntity {
        let userId = user.id.isEmpty ? users.document().documentID : user.id
        let userDocument = users.document(userId)
        let existingSnapshot = try await userDocument.getDocument()
        let existingUser = existingSnapshot
            .data()
            .map { Self.userEntity(id: userId, data: $0) }
        if let existingUser, !existingUser.cardIds.isEmpty {
            return existingUser
        }

        let userWithoutCards = Self.userEntity(
            user: existingUser ?? user,
            id: userId,
            cardIds: []
        )
        if existingUser == nil {
            try await userDocument.setData(Self.dictionary(user: userWithoutCards))
        }

        let cardDocuments = (0..<Self.defaultCardCount).map { index in
            cards.document(Self.defaultCardId(userId: userId, index: index))
        }
        let cardIds = cardDocuments.map(\.documentID)
        let userWithCards = Self.userEntity(
            user: userWithoutCards,
            id: userWithoutCards.id,
            cardIds: cardIds
        )
        let batch = Firestore.firestore().batch()

        for document in cardDocuments {
            batch.setData(
                Self.dictionary(card: Self.defaultCard(id: document.documentID, ownerUid: userId)),
                forDocument: document
            )
        }
        try await batch.commit()

        try await userDocument.setData(Self.dictionary(user: userWithCards))

        return userWithCards
    }

    func getUser(id: String) async throws -> UserEntity {
        let snapshot = try await users.document(id).getDocument()
        guard let data = snapshot.data() else {
            throw RepositoryError.documentNotFound("users/\(id)")
        }
        return Self.userEntity(id: id, data: data)
    }

    func saveUser(user: UserEntity) async throws {
        try await users.document(user.id).setData(Self.dictionary(user: user))
    }

    func updateUser(user: UserEntity) async throws {
        try await saveUser(user: user)
    }

    func deleteUser(id: String) async throws {
        try await users.document(id).delete()
    }

    private static func dictionary(user: UserEntity) -> [String: Any] {
        [
            "id": user.id,
            "createdAt": user.createdAt,
            "updatedAt": user.updatedAt,
            "cardIds": user.cardIds,
        ]
    }

    private static func dictionary(card: CardEntity) -> [String: Any] {
        [
            "id": card.id,
            "ownerUid": card.ownerUid,
            "caption": card.caption,
            "name": cardElementDictionary(element: card.name),
            "email": cardElementDictionary(element: card.email),
            "address1": cardElementDictionary(element: card.address1),
            "address2": cardElementDictionary(element: card.address2),
            "phone": cardElementDictionary(element: card.phone),
            "organization": cardElementDictionary(element: card.organization),
            "title": cardElementDictionary(element: card.title),
            "bgAlpha": card.bgAlpha,
            "bgFile": card.bgFile,
            "createdAt": card.createdAt,
            "updatedAt": card.updatedAt,
            "partnerIds": card.partnerIds,
        ]
    }

    private static func defaultCard(id: String, ownerUid: String) -> CardEntity {
        CardEntity(
            id: id,
            ownerUid: ownerUid,
            caption: "",
            name: cardElement("氏名", defaultValue: "氏名", x: 0.07, y: 0.33, fontSize: 24),
            email: cardElement("mail@example.com", defaultValue: "mail@example.com", x: 0.20, y: 0.66, fontSize: 12),
            address1: cardElement("住所", defaultValue: "住所", x: 0.20, y: 0.77, fontSize: 12),
            address2: cardElement("", defaultValue: "", x: 0.20, y: 0.77, fontSize: 12),
            phone: cardElement("電話番号", defaultValue: "電話番号", x: 0.20, y: 0.55, fontSize: 12),
            organization: cardElement("組織", defaultValue: "組織", x: 0.07, y: 0.12, fontSize: 14),
            title: cardElement("肩書き", defaultValue: "肩書き", x: 0.07, y: 0.23, fontSize: 14),
            bgAlpha: 0.0,
            bgFile: "",
            createdAt: 0,
            updatedAt: 0,
            partnerIds: []
        )
    }

    private static func userEntity(user: UserEntity, id: String, cardIds: [String]) -> UserEntity {
        UserEntity(
            id: id,
            createdAt: user.createdAt,
            updatedAt: user.updatedAt,
            cardIds: cardIds
        )
    }

    private static func userEntity(id: String, data: [String: Any]) -> UserEntity {
        UserEntity(
            id: data["id"] as? String ?? id,
            createdAt: int64Value(data["createdAt"]),
            updatedAt: int64Value(data["updatedAt"]),
            cardIds: data["cardIds"] as? [String] ?? []
        )
    }

    private static let defaultCardCount = 4

    private static func defaultCardId(userId: String, index: Int) -> String {
        "\(userId)_default_card_\(index)"
    }
}

private enum RepositoryError: LocalizedError {
    case documentNotFound(String)

    var errorDescription: String? {
        switch self {
        case let .documentNotFound(path):
            return "Document not found: \(path)"
        }
    }
}

private func int64Value(_ value: Any?) -> Int64 {
    switch value {
    case let value as Int64:
        return value
    case let value as Int:
        return Int64(value)
    case let value as NSNumber:
        return value.int64Value
    default:
        return 0
    }
}

private func floatValue(_ value: Any?) -> Float {
    switch value {
    case let value as Float:
        return value
    case let value as Double:
        return Float(value)
    case let value as Int:
        return Float(value)
    case let value as NSNumber:
        return value.floatValue
    default:
        return 0.0
    }
}

private func cardElementDictionary(element: CardEntity.CardElement) -> [String: Any] {
    [
        "value": element.value,
        "x": element.x,
        "y": element.y,
        "rotation": element.rotation,
        "sanserif": element.sanserif,
        "fontSize": element.fontSize,
    ]
}

private func cardElement(
    _ value: Any?,
    defaultValue: String,
    x: Float,
    y: Float,
    fontSize: Float
) -> CardEntity.CardElement {
    if let value = value as? String {
        return CardEntity.CardElement(
            value: value.isEmpty ? defaultValue : value,
            x: x,
            y: y,
            rotation: 0,
            sanserif: false,
            fontSize: fontSize
        )
    }

    let data = value as? [String: Any] ?? [:]
    let resolvedFontSize = floatValue(data["fontSize"], defaultValue: fontSize)
    let shouldInitializeLayout = resolvedFontSize < 1
    return CardEntity.CardElement(
        value: data["value"] as? String ?? defaultValue,
        x: shouldInitializeLayout ? x : floatValue(data["x"], defaultValue: x),
        y: shouldInitializeLayout ? y : floatValue(data["y"], defaultValue: y),
        rotation: int32Value(data["rotation"]),
        sanserif: data["sanserif"] as? Bool ?? false,
        fontSize: shouldInitializeLayout ? fontSize : resolvedFontSize
    )
}

private func floatValue(_ value: Any?, defaultValue: Float) -> Float {
    switch value {
    case let value as Float:
        return value
    case let value as Double:
        return Float(value)
    case let value as Int:
        return Float(value)
    case let value as NSNumber:
        return value.floatValue
    default:
        return defaultValue
    }
}

private func int32Value(_ value: Any?) -> Int32 {
    switch value {
    case let value as Int32:
        return value
    case let value as Int:
        return Int32(value)
    case let value as NSNumber:
        return value.int32Value
    default:
        return 0
    }
}
