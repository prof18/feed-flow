//
//  FeedConditionalGetURLProtocol.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 12/06/26.
//  Copyright © 2026 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

/// Intercepts the RSS parser's feed requests to perform HTTP conditional GETs.
///
/// Validators (`If-None-Match`/`If-Modified-Since`) are read from the shared
/// `FeedHttpCacheStore`, and response caching headers are recorded back into it so
/// the shared refresh pipeline can schedule the next fetch and handle real 304s.
final class FeedConditionalGetURLProtocol: URLProtocol {
    private static let handledKey = "FeedConditionalGetHandled"
    private var sessionTask: URLSessionDataTask?

    private static let session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = nil
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: configuration)
    }()

    override static func canInit(with request: URLRequest) -> Bool {
        guard URLProtocol.property(forKey: handledKey, in: request) == nil else {
            return false
        }
        guard let scheme = request.url?.scheme?.lowercased() else {
            return false
        }
        return scheme == "http" || scheme == "https"
    }

    override static func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let url = request.url?.absoluteString,
              let mutableRequest = (request as NSURLRequest).mutableCopy() as? NSMutableURLRequest
        else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }

        URLProtocol.setProperty(true, forKey: Self.handledKey, in: mutableRequest)

        let store = Deps.shared.getFeedHttpCacheStore()
        if let validators = store.validatorsFor(url: url) {
            if let etag = validators.etag {
                mutableRequest.setValue(etag, forHTTPHeaderField: "If-None-Match")
            }
            if let lastModified = validators.lastModified {
                mutableRequest.setValue(lastModified, forHTTPHeaderField: "If-Modified-Since")
            }
        }

        let task = Self.session.dataTask(with: mutableRequest as URLRequest) { [weak self] data, response, error in
            guard let self else { return }

            if let error {
                self.client?.urlProtocol(self, didFailWithError: error)
                return
            }

            if let httpResponse = response as? HTTPURLResponse {
                store.recordResponse(
                    url: url,
                    statusCode: Int32(httpResponse.statusCode),
                    etag: httpResponse.value(forHTTPHeaderField: "ETag"),
                    lastModified: httpResponse.value(forHTTPHeaderField: "Last-Modified"),
                    cacheControl: httpResponse.value(forHTTPHeaderField: "Cache-Control"),
                    expires: httpResponse.value(forHTTPHeaderField: "Expires"),
                    date: httpResponse.value(forHTTPHeaderField: "Date"),
                    retryAfter: httpResponse.value(forHTTPHeaderField: "Retry-After")
                )
                self.client?.urlProtocol(self, didReceive: httpResponse, cacheStoragePolicy: .notAllowed)
            } else if let response {
                self.client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            }

            if let data {
                self.client?.urlProtocol(self, didLoad: data)
            }
            self.client?.urlProtocolDidFinishLoading(self)
        }
        sessionTask = task
        task.resume()
    }

    override func stopLoading() {
        sessionTask?.cancel()
        sessionTask = nil
    }
}
