import SwiftUI

extension View {
    func onAppearOrChange<T: Equatable>(_ value: T, perform: @escaping (T) -> Void) -> some View {
        onAppear { perform(value) }
            .onChange(of: value) { perform($1) }
    }
}
