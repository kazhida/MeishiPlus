import AuthenticationServices
import CryptoKit
import FacebookCore
import FacebookLogin
import FirebaseAuth
import FirebaseCore
import Security
import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let authUser: AuthUser
    let userRepository: UserRepository
    let cardRepository: CardRepository
    let userViewModel: UserViewModel
    let onSignOut: (Bool) -> Void
    let onPurchaseCardClick: ((@escaping () -> KotlinUnit) -> Void)
    let onFacebookAuthClick: (@escaping (Account.Facebook) -> Void, @escaping (String) -> Void) -> Void

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authUser: authUser,
            onSignOut: { shouldDeleteData in
                onSignOut(shouldDeleteData.boolValue)
            },
            userRepository: userRepository,
            cardRepository: cardRepository,
            userViewModel: userViewModel,
            onPurchaseCardClick: onPurchaseCardClick,
            onFacebookAuthClick: { onSuccess, onError in
                onFacebookAuthClick(
                    { account in
                        _ = onSuccess(account)
                    },
                    { message in
                        _ = onError(message)
                    }
                )
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {
    }
}

struct ContentView: View {
    @StateObject private var authModel = FirebaseAuthModel()
    private let userRepository: FireStoreUserRepository
    private let cardRepository: FireStoreCardRepository
    private let purchaseManager = IosCardPurchaseManager()
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
                    onSignOut: { shouldDeleteData in
                        authModel.signOut(
                            shouldDeleteData: shouldDeleteData,
                            userRepository: userRepository,
                            cardRepository: cardRepository
                        )
                    },
                    onPurchaseCardClick: { onSuccess in
                        Task {
                            userViewModel.setPurchasing(isPurchasing: true)
                            let purchaseResult = await purchaseManager.purchaseAdditionalCard(
                                authUser: user,
                                userRepository: userRepository,
                                cardRepository: cardRepository
                            )
                            userViewModel.setPurchasing(isPurchasing: false)
                            switch purchaseResult {
                            case .success(let appUser):
                                userViewModel.setAppUser(appUser: appUser)
                                _ = onSuccess()
                            case .failure(let message):
                                userViewModel.setErrorMessage(message: message)
                            case .cancelled:
                                break
                            }
                        }
                    },
                    onFacebookAuthClick: authModel.linkFacebookAccount
                )
                .ignoresSafeArea()
            } else {
                AppleSignInView(
                    isLoading: authModel.isLoading,
                    errorMessage: authModel.errorMessage,
                    onAppleSignInRequest: authModel.prepareAppleSignInRequest,
                    onAppleSignInCompletion: authModel.handleAppleSignInCompletion
                )
            }
        }
    }
}

private struct AppleSignInView: View {
    let isLoading: Bool
    let errorMessage: String?
    let onAppleSignInRequest: (ASAuthorizationAppleIDRequest) -> Void
    let onAppleSignInCompletion: (Result<ASAuthorization, Error>) -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("名刺＋")
                .font(.largeTitle)
                .fontWeight(.semibold)

            Text("Appleアカウントでログイン")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            SignInWithAppleButton(.signIn, onRequest: onAppleSignInRequest, onCompletion: onAppleSignInCompletion)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
            .controlSize(.large)
            .disabled(isLoading)
            .padding(.top, 8)

            if isLoading {
                ProgressView()
                    .padding(.top, 4)
            }

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
private final class FirebaseAuthModel: ObservableObject {
    @Published var currentUser: AuthUser?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private var authStateHandle: AuthStateDidChangeListenerHandle?
    private var currentNonce: String?

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

    func prepareAppleSignInRequest(_ request: ASAuthorizationAppleIDRequest) {
        guard !isLoading else {
            return
        }
        errorMessage = nil

        let nonce = randomNonceString()
        currentNonce = nonce
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
    }

    func handleAppleSignInCompletion(_ result: Result<ASAuthorization, Error>) {
        guard !isLoading else {
            return
        }
        guard FirebaseApp.app() != nil else {
            errorMessage = "GoogleService-Info.plistをiOSターゲットに追加してください。"
            return
        }

        isLoading = true
        errorMessage = nil

        switch result {
        case .failure(let error):
            isLoading = false
            errorMessage = error.localizedDescription
        case .success(let authorization):
            guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
                isLoading = false
                errorMessage = "Apple認証情報を取得できませんでした。"
                return
            }
            guard let nonce = currentNonce else {
                isLoading = false
                errorMessage = "Apple認証の内部状態が不正です。"
                return
            }
            guard let appleIDToken = appleIDCredential.identityToken else {
                isLoading = false
                errorMessage = "Apple IDトークンを取得できませんでした。"
                return
            }
            guard let idTokenString = String(data: appleIDToken, encoding: .utf8) else {
                isLoading = false
                errorMessage = "Apple IDトークンの形式が不正です。"
                return
            }

            let credential = OAuthProvider.appleCredential(
                withIDToken: idTokenString,
                rawNonce: nonce,
                fullName: appleIDCredential.fullName
            )
            Auth.auth().signIn(with: credential) { [weak self] authResult, error in
                Task { @MainActor in
                    guard let self else {
                        return
                    }
                    self.currentNonce = nil
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

    func linkFacebookAccount(
        onSuccess: @escaping (Account.Facebook) -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let currentUser = Auth.auth().currentUser else {
            onError("Facebook認証を連携するFirebaseユーザーが見つかりません。")
            return
        }
        if let providerUserId = currentUser.facebookProviderUserId {
            onSuccess(providerUserId.toFacebookAccount())
            return
        }
        guard let presentingViewController = UIApplication.shared.presentingViewController else {
            onError("Facebook認証画面を表示できませんでした。")
            return
        }
        guard let clientToken = Bundle.main.object(forInfoDictionaryKey: "FacebookClientToken") as? String,
              !clientToken.isEmpty else {
            onError("FacebookClientTokenをInfo.plistに設定してください。")
            return
        }
        let loginManager = LoginManager()
        loginManager.logIn(
            permissions: ["public_profile"],
            from: presentingViewController
        ) { result, error in
            Task { @MainActor in
                if let error {
                    onError(error.localizedDescription)
                    return
                }
                guard let result, !result.isCancelled else {
                    onError("Facebook認証がキャンセルされました。")
                    return
                }
                guard let accessToken = result.token else {
                    onError("Facebookアクセストークンを取得できませんでした。")
                    return
                }
                let credential = FacebookAuthProvider.credential(
                    withAccessToken: accessToken.tokenString
                )
                currentUser.link(with: credential) { _, error in
                    Task { @MainActor in
                        if let error {
                            onError(error.localizedDescription)
                            return
                        }
                        onSuccess(accessToken.userID.toFacebookAccount())
                    }
                }
            }
        }
    }

    func signOut(
        shouldDeleteData: Bool,
        userRepository: UserRepository,
        cardRepository: CardRepository
    ) {
        Task {
            do {
                if shouldDeleteData, let uid = currentUser?.uid {
                    let user = try? await userRepository.getUser(id: uid)
                    let cardIds = Array(Set(user?.cardIds ?? []))
                    for cardId in cardIds {
                        try await cardRepository.deleteCard(id: cardId)
                    }
                    try await userRepository.deleteUser(id: uid)
                }
                try Auth.auth().signOut()
                currentUser = nil
            } catch {
                errorMessage = error.localizedDescription
            }
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

private extension Bool {
    var boolValue: Bool { self }
}

private extension User {
    var facebookProviderUserId: String? {
        providerData.first { $0.providerID == "facebook.com" }?.uid
    }
}

private extension String {
    func toFacebookAccount() -> Account.Facebook {
        Account.Facebook(
            service: "facebook",
            userName: self,
            userUrl: "https://www.facebook.com/\(self)"
        )
    }
}

private func randomNonceString(length: Int = 32) -> String {
    precondition(length > 0)
    let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
    var result = ""
    var remainingLength = length

    while remainingLength > 0 {
        var randomBytes = [UInt8](repeating: 0, count: 16)
        let errorCode = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if errorCode != errSecSuccess {
            fatalError("Unable to generate nonce. OSStatus: \(errorCode)")
        }

        randomBytes.forEach { random in
            if remainingLength == 0 {
                return
            }

            if random < charset.count {
                result.append(charset[Int(random)])
                remainingLength -= 1
            }
        }
    }

    return result
}

private func sha256(_ input: String) -> String {
    let inputData = Data(input.utf8)
    let hashedData = SHA256.hash(data: inputData)
    return hashedData.map { String(format: "%02x", $0) }.joined()
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
