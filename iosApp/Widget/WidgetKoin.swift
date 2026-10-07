import FeedFlowKit
import Foundation

enum WidgetKoin {
    private static let start: Void = {
        let appEnvironment: AppEnvironment
        #if DEBUG
            appEnvironment = AppEnvironment.Debug()
        #else
            appEnvironment = AppEnvironment.Release()
        #endif
        let locale = Locale.current
        _ = doInitKoinIos(
            appEnvironment: appEnvironment,
            languageCode: locale.language.languageCode?.identifier,
            regionCode: locale.region?.identifier,
            dropboxDataSource: DropboxDataSourceIos(),
            googleDrivePlatformClient: GoogleDrivePlatformClient(),
            appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
            telemetry: WidgetTelemetry(),
            notifier: WidgetNotifier(),
            feedUrlProtocolClasses: [FeedConditionalGetURLProtocol.self]
        )
    }()

    static func ensureStarted() {
        _ = start
    }
}
