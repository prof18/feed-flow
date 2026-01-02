import Foundation

enum ImportExportSheetToShow: Identifiable {
    case opmlFilePicker
    case csvFilePicker

    var id: String {
        switch self {
        case .opmlFilePicker:
            return "opmlFilePicker"
        case .csvFilePicker:
            return "csvFilePicker"
        }
    }
}
