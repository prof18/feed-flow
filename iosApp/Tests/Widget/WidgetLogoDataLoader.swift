import Foundation
import Nuke
import os

final class WidgetLogoDataLoader: DataLoading, Cancellable {
    let data: Data?
    private let requests = OSAllocatedUnfairLock(initialState: 0)

    var requestCount: Int {
        requests.withLock { $0 }
    }

    init(data: Data?) {
        self.data = data
    }

    func loadData(
        with request: URLRequest,
        didReceiveData: @escaping @Sendable (Data, URLResponse) -> Void,
        completion: @escaping @Sendable (Error?) -> Void
    ) -> any Cancellable {
        requests.withLock { $0 += 1 }
        if let data, let url = request.url {
            didReceiveData(data, URLResponse(
                url: url,
                mimeType: "image/png",
                expectedContentLength: data.count,
                textEncodingName: nil
            ))
            completion(nil)
        } else {
            completion(URLError(.cannotConnectToHost))
        }
        return self
    }

    func cancel() {}
}
