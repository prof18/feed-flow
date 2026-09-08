import GoogleAPIClientForREST_Drive

struct GoogleDriveServiceClientImpl: GoogleDriveServiceClient {
    let service: GTLRDriveService

    var isAuthorized: Bool {
        service.authorizer != nil
    }

    func executeQuery(
        _ query: GTLRQueryProtocol,
        completionHandler: @escaping (Any?, Any?, Error?) -> Void
    ) {
        service.executeQuery(query) { ticket, result, error in
            completionHandler(ticket, result, error)
        }
    }
}
