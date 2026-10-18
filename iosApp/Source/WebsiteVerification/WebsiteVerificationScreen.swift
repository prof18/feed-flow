import SwiftUI

struct WebsiteVerificationScreen: View {
    let sessionId: String

    var body: some View {
        WebsiteVerificationSessionScreen(sessionId: sessionId)
    }
}
