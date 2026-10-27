/// The feed filter a widget configuration's content id stands for.
struct WidgetContentSelection: Equatable {
    let filterType: String
    let filterId: String?

    init(selectionId: String?) {
        guard let selectionId else {
            self.init(filterType: "timeline", filterId: nil)
            return
        }
        if selectionId == "timeline" || selectionId == "bookmarks" {
            self.init(filterType: selectionId, filterId: nil)
            return
        }

        let categoryPrefix = "category:"
        if selectionId.hasPrefix(categoryPrefix) {
            self.init(filterType: "category", filterId: String(selectionId.dropFirst(categoryPrefix.count)))
            return
        }

        let sourcePrefix = "source:"
        if selectionId.hasPrefix(sourcePrefix) {
            self.init(filterType: "source", filterId: String(selectionId.dropFirst(sourcePrefix.count)))
            return
        }
        self.init(filterType: "timeline", filterId: nil)
    }

    init(filterType: String, filterId: String?) {
        self.filterType = filterType
        self.filterId = filterId
    }
}
