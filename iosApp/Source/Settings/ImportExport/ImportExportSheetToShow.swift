import Foundation

enum ImportExportSheetToShow: Identifiable {
    case filePicker

    var id: String {
        switch self {
        case .filePicker:
            return "filePicker"
        }
    }
}
