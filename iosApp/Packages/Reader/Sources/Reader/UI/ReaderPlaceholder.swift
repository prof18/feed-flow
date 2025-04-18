import Foundation
import SwiftUI

struct ReaderPlaceholder: View {
    public var body: some View {
        GeometryReader { _ in
            VStack(alignment: .leading, spacing: baseFontSize) {
                Color(ReaderTheme.foreground2)
                    .cornerRadius(7)
                    .opacity(0.3)
                    .padding(.top, 5)

                Text("Lorem Ipsum Dolor Sit Amet")
                    .font(.system(size: baseFontSize * 1.5).bold())

                Text("Article Author")
                    .opacity(0.5)
                    .font(.system(size: baseFontSize * 0.833))

                Text(
                    """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce at tortor justo.
                    Donec nec sapien at nunc ullamcorper mattis vel at enim. Ut sollicitudin sed dui a consectetur.
                    Pellentesque eu convallis quam, id accumsan felis. Nunc ornare condimentum lectus,
                    non tristique massa sodales eu. Vivamus tincidunt eget ex et dignissim.
                    In consectetur turpis sit amet pretium volutpat.
                    """)

                Text(
                    """
                    Nulla rhoncus nibh vitae arcu pellentesque congue. Nullam tempor cursus sem eget vehicula.
                    Nulla sit amet enim eu eros finibus suscipit faucibus vel orci. Pellentesque id mollis lorem,
                    id euismod est. Nullam in sapien purus. Nulla sed tellus augue. Mauris aliquet suscipit lectus.
                    """
                )
            }
            .font(.system(size: baseFontSize))
            .multilineTextAlignment(.leading)
            .lineSpacing(baseFontSize * 0.5)
            .frame(maxWidth: 700)
            .frame(maxWidth: .infinity)
            .redacted(reason: .placeholder)
            .opacity(0.3)
        }
        .modifier(ShimmerMask())
        .padding(baseFontSize * 1.5)
        .background(Color(ReaderTheme.background).edgesIgnoringSafeArea(.all))
    }

    private var baseFontSize: CGFloat { 19 }
}

private struct ShimmerMask: ViewModifier {
    var delay: TimeInterval = 1
    private let animation = Animation.easeInOut(duration: 1).repeatForever(autoreverses: false)

    @State private var endState = false

    func body(content: Content) -> some View {
        content
            .mask {
                LinearGradient(
                    colors: [Color.black, Color.black.opacity(0), Color.black],
                    startPoint: startPoint, endPoint: endPoint
                )
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                    withAnimation(animation) {
                        endState.toggle()
                    }
                }
            }
    }

    private var startPoint: UnitPoint {
        .init(x: endState ? 1 : -1, y: 0)
    }

    private var endPoint: UnitPoint {
        .init(x: startPoint.x + 1, y: 0)
    }
}
