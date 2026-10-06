import FeedFlowKit
import Foundation

enum WidgetSupport {
    static var appEnvironment: AppEnvironment {
        #if DEBUG
            return AppEnvironment.Debug()
        #else
            return AppEnvironment.Release()
        #endif
    }

    static var strings: WidgetStrings {
        getWidgetStrings(
            languageCode: Locale.current.language.languageCode?.identifier,
            regionCode: Locale.current.region?.identifier
        )
    }
}
