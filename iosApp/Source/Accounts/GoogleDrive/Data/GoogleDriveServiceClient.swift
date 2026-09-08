import GoogleAPIClientForREST_Drive

protocol GoogleDriveServiceClient {
    var isAuthorized: Bool { get }

    func executeQuery(
        _ query: GTLRQueryProtocol,
        completionHandler: @escaping (Any?, Any?, Error?) -> Void
    )
}
