import FeedFlowKit
import SwiftUI

struct ReaderArticleReportHost: ViewModifier {
    let viewModel: ReaderModeViewModel
    @Binding var state: ArticleReportUiState?

    func body(content: Content) -> some View {
        content
            .modifier(ArticleReportDisclosure(
                state: state,
                onSend: viewModel.confirmArticleReport,
                onCancel: viewModel.cancelArticleReport,
                onDismiss: viewModel.dismissArticleReportMessage
            ))
            .task {
                for await value in viewModel.articleReportState {
                    state = value
                }
            }
    }
}
