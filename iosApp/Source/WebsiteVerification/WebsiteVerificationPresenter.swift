import FeedFlowKit
import SwiftUI
import UIKit

@MainActor
final class WebsiteVerificationPresenter {
    static let shared = WebsiteVerificationPresenter()

    private weak var presentedController: UIViewController?
    private var requestedSessionID: String?
    private var presentationTask: Task<Void, Never>?

    private init() {}

    func update(sessionID: String?) {
        presentationTask?.cancel()
        guard let sessionID else {
            requestedSessionID = nil
            guard let presentedController else { return }
            self.presentedController = nil
            presentedController.dismiss(animated: true)
            return
        }

        if requestedSessionID == sessionID,
           let presentedController,
           presentedController.presentingViewController != nil {
            return
        }
        requestedSessionID = sessionID
        if let presentedController {
            self.presentedController = nil
            presentedController.dismiss(animated: false) { [weak self] in
                self?.present(sessionID: sessionID)
            }
        } else {
            present(sessionID: sessionID)
        }
    }

    private func present(sessionID: String) {
        presentationTask = Task { [weak self] in
            // SwiftUI dismisses its explanation alert after the action callback returns.
            for _ in 0 ..< 20 {
                guard let self, !Task.isCancelled, self.requestedSessionID == sessionID,
                      self.hasForegroundActiveScene else { return }
                if let controller = self.topmostViewController(),
                   !(controller is UIAlertController), !controller.isBeingDismissed,
                   controller.viewIfLoaded?.window != nil {
                    self.presentNow(sessionID: sessionID, from: controller)
                    return
                }
                do { try await Task.sleep(for: .milliseconds(100)) } catch { return }
            }
            // Nothing could host the browser, so release the waiting verification instead of leaving it busy.
            guard let self, self.requestedSessionID == sessionID else { return }
            self.requestedSessionID = nil
            try? await Deps.shared.getFeedRecoveryCoordinator().cancelSession(sessionId: sessionID)
        }
    }

    private func presentNow(sessionID: String, from presentingController: UIViewController) {
        guard requestedSessionID == sessionID,
              hasForegroundActiveScene else { return }

        let controller = UIHostingController(
            rootView: WebsiteVerificationScreen(sessionId: sessionID)
        )
        controller.modalPresentationStyle = .overFullScreen
        presentedController = controller
        presentingController.present(controller, animated: true)
    }

    private func topmostViewController() -> UIViewController? {
        let windowScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        guard let root = windowScene?.windows.first(where: \.isKeyWindow)?.rootViewController else {
            return nil
        }
        return topmostViewController(from: root)
    }

    var hasForegroundActiveScene: Bool {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .contains { $0.activationState == .foregroundActive }
    }

    private func topmostViewController(from controller: UIViewController) -> UIViewController {
        if let presented = controller.presentedViewController {
            return topmostViewController(from: presented)
        }
        if let navigationController = controller as? UINavigationController,
           let visibleController = navigationController.visibleViewController {
            return topmostViewController(from: visibleController)
        }
        if let tabController = controller as? UITabBarController,
           let selectedController = tabController.selectedViewController {
            return topmostViewController(from: selectedController)
        }
        if let splitController = controller as? UISplitViewController,
           let visibleController = splitController.viewControllers.last {
            return topmostViewController(from: visibleController)
        }
        return controller
    }
}
