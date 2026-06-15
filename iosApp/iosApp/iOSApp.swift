import FacebookCore
import FirebaseCore
import Shared
import SwiftUI
import UIKit

@main
struct iOSApp: App {
    init() {
        if FirebaseApp.app() == nil,
           Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }
        ApplicationDelegate.shared.application(
            UIApplication.shared,
            didFinishLaunchingWithOptions: nil
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if ApplicationDelegate.shared.application(
                        UIApplication.shared,
                        open: url,
                        options: [:]
                    ) {
                        return
                    }
                    SnsAuthDeepLinkState.shared.submit(url: url.absoluteString)
                }
        }
    }
}
