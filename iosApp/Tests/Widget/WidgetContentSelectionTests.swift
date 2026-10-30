import XCTest

final class WidgetContentSelectionTests: XCTestCase {
    func testMissingSelectionShowsTheTimeline() {
        XCTAssertEqual(WidgetContentSelection(selectionId: nil), selection("timeline", nil))
    }

    func testBuiltInSelectionsHaveNoFilterId() {
        XCTAssertEqual(WidgetContentSelection(selectionId: "timeline"), selection("timeline", nil))
        XCTAssertEqual(WidgetContentSelection(selectionId: "bookmarks"), selection("bookmarks", nil))
    }

    func testCategoryAndSourceSelectionsKeepTheirId() {
        XCTAssertEqual(WidgetContentSelection(selectionId: "category:42"), selection("category", "42"))
        XCTAssertEqual(WidgetContentSelection(selectionId: "source:abc"), selection("source", "abc"))
    }

    func testOnlyTheFirstPrefixIsStripped() {
        XCTAssertEqual(
            WidgetContentSelection(selectionId: "source:https://example.com/feed"),
            selection("source", "https://example.com/feed")
        )
        XCTAssertEqual(WidgetContentSelection(selectionId: "category:category:1"), selection("category", "category:1"))
    }

    func testUnknownSelectionFallsBackToTheTimeline() {
        for id in ["", "Timeline", "folder:1", "categories:1"] {
            XCTAssertEqual(WidgetContentSelection(selectionId: id), selection("timeline", nil), id)
        }
    }

    func testGroupsSplitBuiltInsCategoriesAndSources() {
        let groups = WidgetContentGroups([
            item("source:1", "Blog"),
            item("timeline", "Timeline"),
            item("category:1", "Tech", isCategory: true),
            item("bookmarks", "Bookmarks"),
            item("other:1", "Ignored")
        ])

        XCTAssertEqual(groups.builtIns.map(\.id), ["timeline", "bookmarks"])
        XCTAssertEqual(groups.categories.map(\.id), ["category:1"])
        XCTAssertEqual(groups.sources.map(\.id), ["source:1"])
    }

    func testCategoriesAndSourcesSortByTitleIgnoringCaseAndDiacritics() {
        let groups = WidgetContentGroups([
            item("category:1", "zeta", isCategory: true),
            item("category:2", "Éclair", isCategory: true),
            item("category:3", "apple", isCategory: true),
            item("source:1", "beta"),
            item("source:2", "Alpha")
        ])

        XCTAssertEqual(groups.categories.map(\.title), ["apple", "Éclair", "zeta"])
        XCTAssertEqual(groups.sources.map(\.title), ["Alpha", "beta"])
    }

    func testBuiltInsKeepTheirOriginalOrder() {
        let groups = WidgetContentGroups([item("bookmarks", "Bookmarks"), item("timeline", "Timeline")])
        XCTAssertEqual(groups.builtIns.map(\.id), ["bookmarks", "timeline"])
    }

    private func selection(_ filterType: String, _ filterId: String?) -> WidgetContentSelection {
        WidgetContentSelection(filterType: filterType, filterId: filterId)
    }

    private func item(_ id: String, _ title: String, isCategory: Bool? = nil) -> TestWidgetContentItem {
        TestWidgetContentItem(id: id, title: title, isCategory: isCategory)
    }
}
