import FirebaseFirestore
import Shared

final class FireStoreCardRepository: CardRepository {
    private let cards = Firestore.firestore().collection("cards")

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

        return try await withThrowingTaskGroup(of: CardEntity.self) { group in
            for id in cardIds {
                group.addTask {
                    try await self.getCard(id: id)
                }
            }

            var results: [CardEntity] = []
            for try await card in group {
                results.append(card)
            }
            return results
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
            "name": card.name,
            "email": card.email,
            "address": card.address,
            "phone": card.phone,
            "organization": card.organization,
            "title": card.title,
            "createdAt": card.createdAt,
            "updatedAt": card.updatedAt,
            "partnerIds": card.partnerIds,
        ]
    }

    private static func cardEntity(id: String, data: [String: Any]) -> CardEntity {
        CardEntity(
            id: data["id"] as? String ?? id,
            name: data["name"] as? String ?? "",
            email: data["email"] as? String ?? "",
            address: data["address"] as? String ?? "",
            phone: data["phone"] as? String ?? "",
            organization: data["organization"] as? String ?? "",
            title: data["title"] as? String ?? "",
            createdAt: int64Value(data["createdAt"]),
            updatedAt: int64Value(data["updatedAt"]),
            partnerIds: data["partnerIds"] as? [String] ?? []
        )
    }
}

final class FireStoreUserRepository: UserRepository {
    private let users = Firestore.firestore().collection("users")

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

    private static func userEntity(id: String, data: [String: Any]) -> UserEntity {
        UserEntity(
            id: data["id"] as? String ?? id,
            createdAt: int64Value(data["createdAt"]),
            updatedAt: int64Value(data["updatedAt"]),
            cardIds: data["cardIds"] as? [String] ?? []
        )
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
