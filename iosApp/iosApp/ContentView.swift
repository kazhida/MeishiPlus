import FacebookCore
import FacebookLogin
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let authUser: AuthUser
    let userRepository: UserRepository
    let cardRepository: CardRepository
    let userViewModel: UserViewModel
    let onSignOut: () -> Void
    let onPurchaseCardClick: ((@escaping () -> KotlinUnit) -> Void)
    let onFacebookAuthClick: (@escaping (Account.Facebook) -> Void, @escaping (String) -> Void) -> Void

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authUser: authUser,
            onSignOut: {
                onSignOut()
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
                    onSignOut: authModel.signOut,
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
