import Shared
import StoreKit

private let cardAddProductId = "card_add_100yen"

enum IosCardPurchaseResult {
    case success(AppUser)
    case failure(String)
    case cancelled
}

final class IosCardPurchaseManager {
    func purchaseAdditionalCard(
        authUser: AuthUser,
        userRepository: FireStoreUserRepository,
        cardRepository: FireStoreCardRepository
    ) async -> IosCardPurchaseResult {
        do {
            let products = try await Product.products(for: [cardAddProductId])
            guard let product = products.first(where: { $0.id == cardAddProductId }) else {
                return .failure("購入対象の商品を取得できませんでした。")
            }

            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                let transaction = try verifiedTransaction(verification)
                let appUser = try await UserInit(
                    userRepository: userRepository,
                    cardRepository: cardRepository
                ).purchaseAdditionalCard(authUser: authUser)
                await transaction.finish()
                return .success(appUser)
            case .userCancelled:
                return .cancelled
            case .pending:
                return .failure("購入が保留中です。")
            @unknown default:
                return .failure("購入に失敗しました。")
            }
        } catch {
            return .failure(error.localizedDescription)
        }
    }

    private func verifiedTransaction(
        _ result: VerificationResult<StoreKit.Transaction>
    ) throws -> StoreKit.Transaction {
        switch result {
        case .verified(let safe):
            return safe
        case .unverified(_, let error):
            throw error
        }
    }
}
