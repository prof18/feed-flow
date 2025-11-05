import SwiftUI

extension View {
    func onAppearOrChange<T: Equatable>(_ value: T, perform: @escaping (T) -> Void) -> some View {
        onAppear { perform(value) }.onChange(of: value, perform: perform)
    }

    @ViewBuilder
    func `if`<NewView: View>(_ condition: Bool, apply: (Self) -> NewView) -> some View {
        if condition {
            apply(self)
        } else {
            self
        }
    }
}
