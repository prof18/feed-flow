import AppIntents
import FeedFlowKit
import Foundation

struct WidgetContentEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Content"
    static let defaultQuery = WidgetContentEntityQuery()

    let id: String
    let title: String
    let subtitle: String?
    let isCategory: Bool?
    var logoUrl: String?
    var logoData: Data?

    var displayRepresentation: DisplayRepresentation {
        let imageName = switch id {
        case "timeline":
            "clock"
        case "bookmarks":
            "bookmark"
        default:
            isCategory == true ? "folder" : WidgetContentLogoLoader.fallbackSymbol
        }
        return DisplayRepresentation(
            title: "\(title)",
            subtitle: subtitle.map { "\($0)" },
            image: logoData.map { .init(data: $0, isTemplate: false) }
                ?? .init(systemName: imageName, isTemplate: true)
        )
    }

    static func timeline(strings: WidgetStrings) -> WidgetContentEntity {
        WidgetContentEntity(
            id: "timeline",
            title: strings.widgetContentTimeline,
            subtitle: nil,
            isCategory: nil
        )
    }
}
