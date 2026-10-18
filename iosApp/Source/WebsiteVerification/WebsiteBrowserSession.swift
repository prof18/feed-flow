import FeedFlowKit
import Foundation
import Observation
import WebKit

@MainActor
@Observable
final class WebsiteBrowserSession: NSObject {
    let sessionId: String
    private(set) var webView: WKWebView
    private(set) var request: WebsiteVerificationRequest?
    private(set) var pageState: WebsiteBrowserPageState = .preparing
    private(set) var isBrowserVisible = false
    private(set) var host = ""
    private(set) var failure: VerificationFailure?

    private var coordinator: FeedRecoveryCoordinator?
    private var repository: FeedRecoveryRepository?
    private var profile: FeedRecoveryProfile?
    private var requestTask: Task<Void, Never>?
    private var timeoutTask: Task<Void, Never>?
    private var captureTask: Task<Void, Never>?
    private var navigationState = WebsiteBrowserNavigationState()
    private var attachLifecycle = WebsiteBrowserAttachLifecycle()
    private var captureOutcome = WebsiteBrowserCaptureOutcome()
    private var submittingCapture = false
    private var closed: Bool { attachLifecycle.isClosed }
    private var approvedURL: URL?
    private var approvedOrigin: String?

    init(sessionId: String) {
        self.sessionId = sessionId
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        webView = WKWebView(frame: .zero, configuration: configuration)
        super.init()
        webView.navigationDelegate = self
        webView.uiDelegate = self
    }

    func start() {
        guard requestTask == nil else { return }
        coordinator = Deps.shared.getFeedRecoveryCoordinator()
        repository = Deps.shared.getFeedRecoveryRepository()
        timeoutTask = Task { [weak self] in
            do {
                try await Task.sleep(
                    for: .milliseconds(Int64(RecoveryPolicy.shared.VERIFICATION_TIMEOUT_MILLIS))
                )
                guard let self, !self.closed else { return }
                await self.fail(VerificationFailure.timeout)
            } catch {
                return
            }
        }
        requestTask = Task { [weak self] in
            guard let self, let coordinator = self.coordinator else { return }
            for await next in coordinator.visibleState {
                guard !Task.isCancelled, !self.closed else { return }
                guard let active = self.verificationRequest(from: next) else { continue }
                await self.handleVisibleRequest(active)
            }
        }
    }

    private func verificationRequest(from state: WebsiteVerificationRequest?) -> WebsiteVerificationRequest? {
        guard let request = state,
              request.sessionId == sessionId else { return nil }
        return request
    }

    private func handleVisibleRequest(_ active: WebsiteVerificationRequest) async {
        await cancelPreviousRead(ifNeeded: active)
        guard attachLifecycle.canContinue(taskIsCancelled: Task.isCancelled) else { return }
        let urlChanged = request?.url != active.url
        if captureOutcome.beginRequest(id: active.id) {
            failure = nil
            switch pageState {
            case .complete, .failed:
                pageState = navigationState.ready ? .checking : .preparing
            case .preparing, .checking, .interactive:
                break
            }
        }
        request = active
        host = URL(string: active.url)?.host ?? active.url
        await prepareOrReload(active, urlChanged: urlChanged)
        applyFailure(active)
        guard navigationState.ready, captureTask == nil, !active.validating, active.failure == nil,
              !captureOutcome.isComplete else { return }
        beginCapture(active)
    }

    private func cancelPreviousRead(ifNeeded active: WebsiteVerificationRequest) async {
        guard request?.id != nil, request?.id != active.id else { return }
        captureTask?.cancel()
        await captureTask?.value
        captureTask = nil
    }

    private func prepareOrReload(_ active: WebsiteVerificationRequest, urlChanged: Bool) async {
        if profile == nil {
            await prepare(active)
        } else if urlChanged {
            load(active)
        }
    }

    private func applyFailure(_ active: WebsiteVerificationRequest) {
        guard let failure = active.failure else { return }
        self.failure = failure
        pageState = .failed
    }

    func close() {
        guard !closed else { return }
        attachLifecycle.close()
        let ownedWebView = activeWebView
        let ownedCaptureTask = captureTask
        let ownedCoordinator = coordinator
        let ownedRepository = repository
        timeoutTask?.cancel()
        requestTask?.cancel()
        ownedCaptureTask?.cancel()
        ownedWebView.stopLoading()
        ownedWebView.navigationDelegate = nil
        ownedWebView.uiDelegate = nil
        ownedWebView.evaluateJavaScript(
            "globalThis.__feedFlowTransfer?.cancel(globalThis.__feedFlowTransferNonce);",
            completionHandler: nil
        )
        releaseWebView(ownedWebView)
        Task {
            if let ownedCoordinator {
                try? await ownedCoordinator.cancelSession(sessionId: sessionId)
            }
            await self.requestTask?.value
            await ownedCaptureTask?.value
            if let ownedProfile = self.profile, let ownedRepository {
                try? await ownedRepository.retireOrphan(profileId: ownedProfile.id)
            }
            // Let SwiftUI drop its last reference to the web view before the data store is removed.
            try? await Task.sleep(for: .milliseconds(500))
            try? await WebsiteProfileStore.shared.removeRetiredProfiles()
        }
    }

    // A data store with a live web view cannot be removed, so detach the profile-bound view first.
    private func releaseWebView(_ released: WKWebView) {
        released.removeFromSuperview()
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        webView = WKWebView(frame: .zero, configuration: configuration)
    }

    func retry() {
        guard let request, !closed, captureTask == nil, !request.validating else { return }
        failure = nil
        beginCapture(request)
    }

    /// Re-checks while the challenge is on screen, for checks that finish without a page navigation.
    /// The browser stays visible, and only a successful capture changes what the user sees.
    func recheckSilently() {
        guard let request, !closed, pageState == .interactive, captureTask == nil, !request.validating else { return }
        beginCapture(request, silent: true)
    }

    private func prepare(_ active: WebsiteVerificationRequest) async {
        guard let coordinator, let repository,
              let url = URL(string: active.url),
              let origin = WebsiteProfileIdentity.origin(url), origin == active.origin
        else {
            await fail(VerificationFailure.unsupported)
            return
        }
        var candidateWebView: WKWebView?
        do {
            try? await WebsiteProfileStore.shared.removeRetiredProfiles()
            let userAgent = try await WebsiteProfileStore.shared.userAgent()
            let profile = try await repository.acquireProfileForUrl(
                epoch: active.accountEpoch,
                url: active.url,
                userAgent: userAgent,
                engineVersion: WebsiteProfileStore.engineVersion
            )
            self.profile = profile
            guard !closed, try await repository.isCurrent(profile: profile).boolValue,
                  let identifier = WebsiteProfileIdentity.identifier(profile)
            else {
                await fail(VerificationFailure.unsupported)
                return
            }
            let store = WKWebsiteDataStore(forIdentifier: identifier)
            let ownedWebView = makeWebView(store: store)
            candidateWebView = ownedWebView
            webView.stopLoading()
            webView.navigationDelegate = nil
            webView.uiDelegate = nil
            approvedURL = url
            approvedOrigin = origin
            try await coordinator.attachProfile(sessionId: sessionId, profile: profile)
            guard attachLifecycle.canContinue(taskIsCancelled: Task.isCancelled) else {
                dispose(candidateWebView)
                return
            }
            webView = ownedWebView
            candidateWebView = nil
            pageState = .checking
            ownedWebView.load(URLRequest(url: url))
        } catch is CancellationError {
            dispose(candidateWebView)
            return
        } catch {
            dispose(candidateWebView)
            await fail(VerificationFailure.unsupported)
        }
    }

    private func dispose(_ candidate: WKWebView?) {
        candidate?.stopLoading()
        candidate?.navigationDelegate = nil
        candidate?.uiDelegate = nil
    }

    private func makeWebView(store: WKWebsiteDataStore) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = store
        let browser = WKWebView(frame: .zero, configuration: configuration)
        #if DEBUG
            browser.isInspectable = true
        #endif
        browser.navigationDelegate = self
        browser.uiDelegate = self
        return browser
    }

    private var activeWebView: WKWebView {
        webView
    }

    private func load(_ active: WebsiteVerificationRequest) {
        guard attachLifecycle.canContinue(taskIsCancelled: Task.isCancelled) else { return }
        guard let url = URL(string: active.url), WebsiteProfileIdentity.origin(url) == approvedOrigin else {
            Task { await fail(.unsupported) }
            return
        }
        approvedURL = url
        navigationState.started(nil)
        pageState = isBrowserVisible ? .interactive : .checking
        activeWebView.load(URLRequest(url: url))
    }
}

extension WebsiteBrowserSession {
    private func beginCapture(_ active: WebsiteVerificationRequest, silent: Bool = false) {
        guard canBeginCapture(active), let profile, let repository, let coordinator else { return }
        let generation = navigationState.generation
        if !silent {
            isBrowserVisible = false
            pageState = .checking
        }
        captureTask = Task { [weak self] in
            guard let self else { return }
            defer {
                self.captureTask = nil
                self.submittingCapture = false
            }
            do {
                let capture = try await self.captureResponse(active, profile: profile, repository: repository)
                try Task.checkCancellation()
                guard self.isCurrentCapture(requestID: active.id, generation: generation) else { return }
                // A completed response belongs to this request even if the page reloads while it is submitted.
                self.submittingCapture = true
                guard try await coordinator.startRead(id: active.id).boolValue else { return }
                try await self.completeCapture(capture, requestID: active.id, profile: profile, coordinator: coordinator)
            } catch is CancellationError {
                return
            } catch {
                // A background re-check never ends the session: the challenge simply stays on screen.
                guard !silent, self.isCurrentCapture(requestID: active.id, generation: generation) else { return }
                await self.handleCaptureError(error)
            }
        }
    }

    private func handleCaptureError(_ error: Error) async {
        if let transfer = error as? WebsiteBrowserTransferError {
            await handle(transfer)
        } else {
            await fail(VerificationFailure.invalidContent)
        }
    }

    private func captureResponse(
        _ active: WebsiteVerificationRequest,
        profile: FeedRecoveryProfile,
        repository: FeedRecoveryRepository
    ) async throws -> WebsiteBrowserCapturedResponse {
        let currentProfile = try await repository.isCurrent(profile: profile).boolValue
        try Task.checkCancellation()
        guard currentProfile, let origin = approvedOrigin else {
            throw WebsiteBrowserTransferError(reason: "owner")
        }
        return try await WebsiteBrowserTransfer(
            webView: activeWebView, approvedOrigin: origin
        ).capture(urlString: active.url)
    }

    private func canBeginCapture(_ active: WebsiteVerificationRequest) -> Bool {
        !closed && captureTask == nil && !active.validating && active.failure == nil && !captureOutcome.isComplete
    }

    private func isCurrentCapture(requestID: String, generation: Int) -> Bool {
        !Task.isCancelled && !closed && request?.id == requestID
            && navigationState.canCapture(generation: generation)
    }

    private func completeCapture(
        _ capture: WebsiteBrowserCapturedResponse,
        requestID: String,
        profile: FeedRecoveryProfile,
        coordinator: FeedRecoveryCoordinator
    ) async throws {
        guard !closed, request?.id == requestID else { return }
        guard isValid(capture) else {
            await fail(.invalidContent)
            return
        }
        let response = Deps.shared.browserCapture(
            url: capture.effectiveURL,
            status: Int32(capture.statusCode),
            contentType: capture.contentType,
            data: capture.data
        )
        try await coordinator.captured(id: requestID, response: response, profile: profile)
        guard captureOutcome.complete(requestID: requestID) else { return }
        pageState = .complete
        isBrowserVisible = false
    }

    private func isValid(_ capture: WebsiteBrowserCapturedResponse) -> Bool {
        guard !closed, let responseURL = URL(string: capture.effectiveURL) else { return false }
        return WebsiteProfileIdentity.origin(responseURL) == approvedOrigin
            && capture.effectiveURL == approvedURL?.absoluteString
            && (200 ... 299).contains(capture.statusCode)
            && capture.data.count <= Int(RecoveryPolicy.shared.MAX_BODY_BYTES)
    }

    private func handle(_ transfer: WebsiteBrowserTransferError) async {
        guard transfer.reason == "challenge" else {
            await fail(transfer.failure)
            return
        }
        isBrowserVisible = true
        pageState = .interactive
    }

    private func fail(_ failure: VerificationFailure) async {
        guard !closed else { return }
        self.failure = failure
        pageState = .failed
        if let request, let coordinator {
            try? await coordinator.failed(id: request.id, reason: failure)
        }
        captureTask?.cancel()
        webView.stopLoading()
        isBrowserVisible = false
    }
}

extension WebsiteBrowserSession: WKNavigationDelegate, WKUIDelegate {
    func webView(
        _: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let targetFrame = navigationAction.targetFrame,
              let url = navigationAction.request.url
        else {
            decisionHandler(.cancel)
            return
        }
        let safeNavigation = WebsiteBrowserNavigationPolicy.allows(
            url: url,
            isMainFrame: targetFrame.isMainFrame,
            approvedOrigin: approvedOrigin,
            origin: WebsiteProfileIdentity.origin
        )
        guard safeNavigation else { decisionHandler(.cancel); return }
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        guard !closed, webView === activeWebView else { return }
        navigationState.started(navigation)
        if !submittingCapture { captureTask?.cancel() }
        pageState = isBrowserVisible ? .interactive : .checking
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        guard !closed, webView === activeWebView, navigationState.finished(navigation) else { return }
        resumeCaptureAfterNavigation()
    }

    private func resumeCaptureAfterNavigation() {
        guard let request, !request.validating, request.failure == nil else { return }
        let generation = navigationState.generation
        if let captureTask {
            Task {
                await captureTask.value
                guard self.navigationState.canCapture(generation: generation), !self.closed,
                      let active = self.request, active.id == request.id, !active.validating,
                      !self.captureOutcome.isComplete else { return }
                self.beginCapture(active)
            }
        } else {
            beginCapture(request)
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        guard !closed, webView === activeWebView else { return }
        let nativeError = error as NSError
        Deps.shared.getLogger(tag: "WebsiteBrowserSession").d(
            messageString: "Navigation error: \(nativeError.domain), code \(nativeError.code)"
        )
        let failed = navigationState.failed(navigation, error: error)
        guard !submittingCapture else { return }
        guard failed else {
            if navigationState.ready { resumeCaptureAfterNavigation() }
            return
        }
        captureTask?.cancel()
        if let request, let coordinator {
            Task { try? await coordinator.failed(id: request.id, reason: .browserStopped) }
        }
        failure = .browserStopped
        pageState = .failed
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        self.webView(webView, didFail: navigation, withError: error)
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        guard !closed, webView === activeWebView else { return }
        captureTask?.cancel()
        if let request, let coordinator {
            Task { try? await coordinator.failed(id: request.id, reason: .browserStopped) }
        }
        failure = .browserStopped
        pageState = .failed
    }

    func webView(
        _: WKWebView,
        createWebViewWith _: WKWebViewConfiguration,
        for _: WKNavigationAction,
        windowFeatures _: WKWindowFeatures
    ) -> WKWebView? {
        nil
    }
}
