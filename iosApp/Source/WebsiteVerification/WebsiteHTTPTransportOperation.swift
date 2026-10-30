import Foundation

final class WebsiteHTTPTransportOperation: NSObject, URLSessionDataDelegate, URLSessionTaskDelegate {
    private let maxBytes: Int
    private let protocolClasses: [AnyClass]?
    private let lock = NSLock()
    private let delegateQueue: OperationQueue = {
        let queue = OperationQueue()
        queue.name = "FeedFlow.WebsiteHTTPTransport"
        queue.maxConcurrentOperationCount = 1
        return queue
    }()

    private var continuation: CheckedContinuation<WebsiteHTTPResponse, Error>?
    private var session: URLSession?
    private var task: URLSessionDataTask?
    private var response: HTTPURLResponse?
    private var receivedData = Data()
    private var finished = false

    init(maxBytes: Int, protocolClasses: [AnyClass]?) {
        self.maxBytes = maxBytes
        self.protocolClasses = protocolClasses
    }

    func fetch(request: URLRequest) async throws -> WebsiteHTTPResponse {
        try await withCheckedThrowingContinuation { continuation in
            lock.lock()
            guard !finished else {
                lock.unlock()
                continuation.resume(throwing: CancellationError())
                return
            }
            self.continuation = continuation
            lock.unlock()

            let configuration = URLSessionConfiguration.ephemeral
            configuration.httpCookieStorage = nil
            configuration.httpCookieAcceptPolicy = .never
            configuration.urlCredentialStorage = nil
            configuration.urlCache = nil
            configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
            configuration.timeoutIntervalForRequest = 30
            configuration.timeoutIntervalForResource = 30
            configuration.protocolClasses = protocolClasses
            let session = URLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)
            lock.lock()
            if finished {
                lock.unlock()
                session.invalidateAndCancel()
                return
            }
            self.session = session
            let task = session.dataTask(with: request)
            self.task = task
            lock.unlock()
            task.resume()
        }
    }

    func cancel() {
        finish(.failure(CancellationError()), cancelTask: true)
    }

    func urlSession(
        _: URLSession,
        dataTask _: URLSessionDataTask,
        didReceive response: URLResponse,
        completionHandler: @escaping (URLSession.ResponseDisposition) -> Void
    ) {
        guard let httpResponse = response as? HTTPURLResponse else {
            completionHandler(.cancel)
            finish(.failure(WebsiteHTTPTransportError.invalidURL), cancelTask: true)
            return
        }
        lock.lock()
        guard !finished else {
            lock.unlock()
            completionHandler(.cancel)
            return
        }
        self.response = httpResponse
        lock.unlock()
        let challenge = Self.challenge(in: httpResponse)
        if challenge {
            completionHandler(.cancel)
            finish(
                .success(makeResponse(httpResponse, data: Data(), challenge: true, tooLarge: false)),
                cancelTask: true
            )
            return
        }
        if httpResponse.expectedContentLength > Int64(maxBytes) {
            completionHandler(.cancel)
            finish(
                .success(makeResponse(httpResponse, data: Data(), challenge: false, tooLarge: true)),
                cancelTask: true
            )
            return
        }
        if !(200 ... 299).contains(httpResponse.statusCode) {
            completionHandler(.cancel)
            finish(
                .success(makeResponse(httpResponse, data: Data(), challenge: false, tooLarge: false)),
                cancelTask: true
            )
            return
        }
        completionHandler(.allow)
    }

    func urlSession(_: URLSession, dataTask _: URLSessionDataTask, didReceive data: Data) {
        lock.lock()
        guard !finished else {
            lock.unlock()
            return
        }
        guard receivedData.count <= maxBytes - data.count else {
            let response = self.response
            lock.unlock()
            if let response {
                finish(
                    .success(makeResponse(response, data: Data(), challenge: false, tooLarge: true)),
                    cancelTask: true
                )
            } else {
                finish(.failure(WebsiteHTTPTransportError.invalidURL), cancelTask: true)
            }
            return
        }
        receivedData.append(data)
        lock.unlock()
    }

    func urlSession(
        _: URLSession,
        task _: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest _: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        // Session requests carry a saved identity, so a redirect is never followed
        completionHandler(nil)
        finish(
            .success(makeResponse(response, data: Data(), challenge: Self.challenge(in: response), tooLarge: false)),
            cancelTask: true
        )
    }

    func urlSession(_: URLSession, task _: URLSessionTask, didCompleteWithError error: Error?) {
        if let error {
            finish(.failure(WebsiteHTTPTransportError.transport(error)), cancelTask: false)
        } else {
            lock.lock()
            let response = self.response
            let data = receivedData
            lock.unlock()
            if let response {
                finish(
                    .success(makeResponse(response, data: data, challenge: false, tooLarge: false)),
                    cancelTask: false
                )
            } else {
                finish(.failure(WebsiteHTTPTransportError.invalidURL), cancelTask: false)
            }
        }
    }

    private func finish(_ result: Result<WebsiteHTTPResponse, Error>, cancelTask: Bool) {
        lock.lock()
        guard !finished else {
            lock.unlock()
            return
        }
        finished = true
        let continuation = self.continuation
        self.continuation = nil
        let task = self.task
        let session = self.session
        self.task = nil
        self.session = nil
        lock.unlock()

        if cancelTask {
            task?.cancel()
        }
        session?.invalidateAndCancel()
        continuation?.resume(with: result)
    }

    private func makeResponse(
        _ response: HTTPURLResponse,
        data: Data,
        challenge: Bool,
        tooLarge: Bool
    ) -> WebsiteHTTPResponse {
        let headers = Dictionary(
            response.allHeaderFields.map { (String(describing: $0.key), String(describing: $0.value)) },
            uniquingKeysWith: { first, _ in first }
        )
        return WebsiteHTTPResponse(
            effectiveURL: response.url ?? URL(fileURLWithPath: "/"),
            statusCode: response.statusCode,
            headers: headers,
            data: data,
            challenge: challenge,
            tooLarge: tooLarge
        )
    }

    private static func challenge(in response: HTTPURLResponse) -> Bool {
        response.allHeaderFields.contains {
            String(describing: $0.key).caseInsensitiveCompare("cf-mitigated") == .orderedSame
                && String(describing: $0.value).trimmingCharacters(in: .whitespacesAndNewlines) == "challenge"
        }
    }
}
