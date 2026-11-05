enum DisconnectAccountKind {
    case readerService
    case backupService

    var message: String {
        switch self {
        case .readerService: feedFlowStrings.accountDisconnectServerMessage
        case .backupService: feedFlowStrings.accountDisconnectBackupMessage
        }
    }
}
