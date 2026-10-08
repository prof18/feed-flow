import SwiftUI

struct ReaderBottomAccessoryModifier<Accessory: View>: ViewModifier {
    let accessory: Accessory
    let isVisible: Bool

    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.safeAreaBar(edge: .bottom, spacing: isVisible ? 10 : 0) {
                if isVisible {
                    accessory
                }
            }
        } else {
            content
        }
    }
}
