import AppIntents
import FeedFlowKit

struct WidgetContentEntityQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [WidgetContentEntity] {
        try await withLogos(allEntities().filter { identifiers.contains($0.id) })
    }

    func suggestedEntities() async throws -> IntentItemCollection<WidgetContentEntity> {
        try collection(for: await withLogos(allEntities()))
    }

    private func collection(for entities: [WidgetContentEntity]) -> IntentItemCollection<WidgetContentEntity> {
        let strings = WidgetSupport.strings
        let groups = WidgetContentGroups(entities)

        var sections: [IntentItemSection<WidgetContentEntity>] = []
        if !groups.builtIns.isEmpty {
            sections.append(IntentItemSection("\(strings.widgetContentSectionTitle)", items: groups.builtIns))
        }
        if !groups.categories.isEmpty {
            sections.append(IntentItemSection("\(strings.widgetContentCategory)", items: groups.categories))
        }
        if !groups.sources.isEmpty {
            sections.append(IntentItemSection("\(strings.widgetContentFeedSource)", items: groups.sources))
        }
        return IntentItemCollection(sections: sections)
    }

    func entities(matching string: String) async throws -> IntentItemCollection<WidgetContentEntity> {
        let query = string.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return try await suggestedEntities() }

        let matches = allEntities().filter { entity in
            entity.title.range(
                of: query,
                options: [.caseInsensitive, .diacriticInsensitive],
                locale: .current
            ) != nil || entity.subtitle?.range(
                of: query,
                options: [.caseInsensitive, .diacriticInsensitive],
                locale: .current
            ) != nil
        }
        return try collection(for: await withLogos(matches))
    }

    func defaultResult() async -> WidgetContentEntity? {
        WidgetContentEntity.timeline(strings: WidgetSupport.strings)
    }

    private func allEntities() -> [WidgetContentEntity] {
        let strings = WidgetSupport.strings
        var entities = [
            WidgetContentEntity.timeline(strings: strings),
            WidgetContentEntity(
                id: "bookmarks",
                title: strings.widgetContentBookmarks,
                subtitle: nil,
                isCategory: nil
            )
        ]
        entities += getWidgetContentOptions(
            appEnvironment: WidgetSupport.appEnvironment
        ).map { option in
            let prefix = option.isCategory ? "category" : "source"
            return WidgetContentEntity(
                id: "\(prefix):\(option.id)",
                title: option.title,
                subtitle: option.subtitle,
                isCategory: option.isCategory,
                logoUrl: option.logoUrl
            )
        }
        return entities
    }

    private func withLogos(_ entities: [WidgetContentEntity]) async throws -> [WidgetContentEntity] {
        try Task.checkCancellation()
        let indices = entities.indices.filter { entities[$0].logoUrl != nil }
        guard !indices.isEmpty else { return entities }

        let result = await withTaskGroup(of: (Int?, Data?).self) { group in
            // Bound the entire query, not just each request, so an offline library opens promptly.
            group.addTask {
                try? await Task.sleep(for: .seconds(2))
                return (nil, nil)
            }
            var nextIndex = 0
            for index in indices.prefix(4) {
                let url = entities[index].logoUrl
                group.addTask { (index, await WidgetContentLogoLoader.imageData(for: url)) }
                nextIndex += 1
            }

            var enriched = entities
            var completed = 0
            for await(index, data) in group {
                guard let index, !Task.isCancelled else { break }
                enriched[index].logoData = data
                completed += 1
                if nextIndex < indices.count {
                    let index = indices[nextIndex]
                    let url = entities[index].logoUrl
                    group.addTask { (index, await WidgetContentLogoLoader.imageData(for: url)) }
                    nextIndex += 1
                }
                if completed == indices.count {
                    break
                }
            }
            group.cancelAll()
            return enriched
        }
        try Task.checkCancellation()
        return result
    }
}
