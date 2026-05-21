import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import SwiftUI
import Shared
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let authUser: AuthUser
    let onSignOut: () -> Void

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authUser: authUser,
            onSignOut: {
                onSignOut()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {
    }
}

struct ContentView: View {
    @StateObject private var authModel = FirebaseAuthModel()

    var body: some View {
        Group {
            if let user = authModel.currentUser {
                ComposeView(
                    authUser: user,
                    onSignOut: authModel.signOut
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
