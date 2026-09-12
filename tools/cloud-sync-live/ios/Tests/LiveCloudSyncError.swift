import Foundation

enum LiveCloudSyncError: LocalizedError {
    case invalidConfiguration(String)
    case missingTarget
    case targetNotFound(String)
    case targetNotHittable(String)
    case ambiguousTarget(String)
    case unsupportedAction(String)
    case missingText

    var errorDescription: String? {
        switch self {
        case let .invalidConfiguration(message):
            message
        case .missingTarget:
            "This action requires a non-empty target."
        case let .targetNotFound(target):
            "No element has the exact accessibility identifier or label '\(target)'."
        case let .targetNotHittable(target):
            "The element '\(target)' exists but is not hittable."
        case let .ambiguousTarget(target):
            "More than one hittable element matches '\(target)'; use a unique accessibility identifier."
        case let .unsupportedAction(action):
            "Unsupported action '\(action)'."
        case .missingText:
            "typeText requires a text value."
        }
    }
}
