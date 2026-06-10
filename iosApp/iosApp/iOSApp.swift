import FirebaseCore
import GoogleSignIn
import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        if FirebaseApp.app() == nil,
           Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if GIDSignIn.sharedInstance.handle(url) {
                        return
                    }
                    SnsAuthDeepLinkState.shared.submit(url: url.absoluteString)
                }
        }
    }
}
