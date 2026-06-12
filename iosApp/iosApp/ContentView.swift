import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import Shared
import StoreKit
import SwiftUI
import UIKit

private let cardAddProductId = "card_add_100yen"

struct ComposeView: UIViewControllerRepresentable {
    let authUser: AuthUser
    let userRepository: UserRepository
    let cardRepository: CardRepository
    let userViewModel: UserViewModel
    let onSignOut: () -> Void
    let onPurchaseCardClick: ((@escaping () -> KotlinUnit) -> Void)

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authUser: authUser,
            onSignOut: {
                onSignOut()
            },
            userRepository: userRepository,
            cardRepository: cardRepository,
            userViewModel: userViewModel,
            onPurchaseCardClick: onPurchaseCardClick
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {
    }
}

struct ContentView: View {
    @StateObject private var authModel = FirebaseAuthModel()
    private let userRepository: FireStoreUserRepository
    private let cardRepository: FireStoreCardRepository
    @State private var userViewModel: UserViewModel

    init() {
        let userRepository = FireStoreUserRepository()
        let cardRepository = FireStoreCardRepository()
        self.userRepository = userRepository
        self.cardRepository = cardRepository
        _userViewModel = State(
            initialValue: UserViewModel(
                userInit: UserInit(userRepository: userRepository, cardRepository: cardRepository),
                cardRepository: cardRepository
            )
        )
    }

    var body: some View {
        Group {
            if let user = authModel.currentUser {
                ComposeView(
                    authUser: user,
                    userRepository: userRepository,
                    cardRepository: cardRepository,
                    userViewModel: userViewModel,
                    onSignOut: authModel.signOut,
                    onPurchaseCardClick: { onSuccess in
                        Task {
                            await purchaseAdditionalCard(
                                authUser: user,
                                userRepository: userRepository,
                                cardRepository: cardRepository,
                                userViewModel: userViewModel,
                                onSuccess: onSuccess,
                                onFailure: { message in
                                    userViewModel.setErrorMessage(message: message)
                                    userViewModel.setPurchasing(isPurchasing: false)
                                }
                            )
                        }
                    }
                )
                .ignoresSafeArea()
            } else {
                GoogleSignInView(
                    isLoading: authModel.isLoading,
                    errorMessage: authModel.errorMessage,
                    onSignIn: authModel.signInWithGoogle
                )
            }
        }
    }
}

private struct GoogleSignInView: View {
    let isLoading: Bool
    let errorMessage: String?
    let onSignIn: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("名刺＋")
                .font(.largeTitle)
                .fontWeight(.semibold)

            Text("Googleアカウントでログイン")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button(action: onSignIn) {
                if isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Googleでログイン")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(isLoading)
            .padding(.top, 8)

            if let errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .padding(.top, 4)
            }
        }
        .padding(24)
    }
}

@MainActor
private func purchaseAdditionalCard(
    authUser: AuthUser,
    userRepository: FireStoreUserRepository,
    cardRepository: FireStoreCardRepository,
    userViewModel: UserViewModel,
    onSuccess: @escaping () -> KotlinUnit,
    onFailure: @escaping (String) -> Void
) async {
    do {
        userViewModel.setPurchasing(isPurchasing: true)
        let products = try await Product.products(for: [cardAddProductId])
        guard let product = products.first else {
            onFailure("購入対象の商品を取得できませんでした。")
            return
        }

        let result = try await product.purchase()
        switch result {
        case .success(let verification):
            let transaction = try verifiedTransaction(verification)
            let appUser = try await purchaseAdditionalCard(
                authUser: authUser,
                userRepository: userRepository,
                cardRepository: cardRepository
            )
            userViewModel.setAppUser(appUser: appUser)
            userViewModel.setPurchasing(isPurchasing: false)
            onSuccess()
            await transaction.finish()
        case .userCancelled:
            userViewModel.setPurchasing(isPurchasing: false)
        case .pending:
            onFailure("購入が保留中です。")
        @unknown default:
            onFailure("購入に失敗しました。")
        }
    } catch {
        onFailure(error.localizedDescription)
    }
}

private func purchaseAdditionalCard(
    authUser: AuthUser,
    userRepository: FireStoreUserRepository,
    cardRepository: FireStoreCardRepository
) async throws -> AppUser {
    let userEntity = (try? await userRepository.getUser(id: authUser.uid))
        ?? UserEntity(
            id: authUser.uid,
            createdAt: 0,
            updatedAt: 0,
            accounts: [],
            cardIds: []
        )
    let cards = userEntity.cardIds.isEmpty
        ? []
        : try await cardRepository.getCards(cardIds: userEntity.cardIds)
    guard let sourceCard = cards.first else {
        throw NSError(domain: "MeishiPlus", code: 1, userInfo: [NSLocalizedDescriptionKey: "追加対象のカードがありません。"])
    }
    let addedCard = try await cardRepository.addCard(
        card: CardEntity(
            id: "",
            ownerUid: authUser.uid,
            caption: sourceCard.caption,
            name: sourceCard.name,
            email: sourceCard.email,
            address1: sourceCard.address1,
            address2: sourceCard.address2,
            phone: sourceCard.phone,
            organization: sourceCard.organization,
            title: sourceCard.title,
            bgAlpha: sourceCard.bgAlpha,
            bgFile: sourceCard.bgFile,
            createdAt: sourceCard.createdAt,
            updatedAt: sourceCard.updatedAt,
            remark: sourceCard.remark,
            accounts: sourceCard.accounts,
            partnerIds: sourceCard.partnerIds
        )
    )
    let updatedUser = UserEntity(
        id: userEntity.id,
        createdAt: userEntity.createdAt,
        updatedAt: userEntity.updatedAt,
        accounts: userEntity.accounts,
        cardIds: userEntity.cardIds + [addedCard.id]
    )
    try await userRepository.saveUser(user: updatedUser)
    return AppUser(user: updatedUser, cards: cards + [addedCard])
}

private func verifiedTransaction(_ result: VerificationResult<StoreKit.Transaction>) throws -> StoreKit.Transaction {
    switch result {
    case .verified(let safe):
        return safe
    case .unverified(_, let error):
        throw error
    }
}

@MainActor
private final class FirebaseAuthModel: ObservableObject {
    @Published var currentUser: AuthUser?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private var authStateHandle: AuthStateDidChangeListenerHandle?

    init() {
        guard FirebaseApp.app() != nil else {
            errorMessage = "GoogleService-Info.plistをiOSターゲットに追加してください。"
            return
        }

        authStateHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in
                self?.currentUser = user?.toSharedAuthUser()
            }
        }
    }

    deinit {
        if let authStateHandle {
            Auth.auth().removeStateDidChangeListener(authStateHandle)
        }
    }

    func signInWithGoogle() {
        guard !isLoading else {
            return
        }
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            errorMessage = "FirebaseのclientIDを取得できませんでした。"
            return
        }
        guard let presentingViewController = UIApplication.shared.presentingViewController else {
            errorMessage = "ログイン画面を表示できませんでした。"
            return
        }

        isLoading = true
        errorMessage = nil
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { [weak self] result, error in
            Task { @MainActor in
                guard let self else {
                    return
                }
                if let error {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                    return
                }
                guard let user = result?.user,
                      let idToken = user.idToken?.tokenString else {
                    self.isLoading = false
                    self.errorMessage = "Google IDトークンを取得できませんでした。"
                    return
                }

                let credential = GoogleAuthProvider.credential(
                    withIDToken: idToken,
                    accessToken: user.accessToken.tokenString
                )
                Auth.auth().signIn(with: credential) { authResult, error in
                    Task { @MainActor in
                        self.isLoading = false
                        if let error {
                            self.errorMessage = error.localizedDescription
                            return
                        }
                        self.currentUser = authResult?.user.toSharedAuthUser()
                    }
                }
            }
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        do {
            try Auth.auth().signOut()
            currentUser = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private extension User {
    func toSharedAuthUser() -> AuthUser {
        AuthUser(
            uid: uid,
            displayName: displayName,
            email: email,
            photoUrl: photoURL?.absoluteString
        )
    }
}

private extension Optional where Wrapped == User {
    func toSharedAuthUser() -> AuthUser? {
        self?.toSharedAuthUser()
    }
}

private extension UIApplication {
    var presentingViewController: UIViewController? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController?
            .topPresentedViewController
    }
}

private extension UIViewController {
    var topPresentedViewController: UIViewController {
        if let presentedViewController {
            return presentedViewController.topPresentedViewController
        }
        if let navigationController = self as? UINavigationController {
            return navigationController.visibleViewController?.topPresentedViewController ?? navigationController
        }
        if let tabBarController = self as? UITabBarController {
            return tabBarController.selectedViewController?.topPresentedViewController ?? tabBarController
        }
        return self
    }
}
