import Foundation
import Testing

struct SnackbarPresentationTests {
    @Test
    func deepestSheetOwnsPresentation() {
        let presentation = SnackbarPresentation()
        let parent = UUID()
        let child = UUID()

        #expect(presentation.frontmostSurface == nil)
        presentation.register(parent, depth: 1)
        presentation.register(child, depth: 2)
        presentation.unregister(parent)
        presentation.register(parent, depth: 1)
        #expect(presentation.frontmostSurface == child)
        presentation.unregister(child)
        presentation.unregister(child)
        #expect(presentation.frontmostSurface == parent)
        presentation.unregister(parent)
        #expect(presentation.frontmostSurface == nil)
    }

    @Test
    func laterSiblingWinsWithoutDuplicateRegistration() {
        let presentation = SnackbarPresentation()
        let first = UUID()
        let second = UUID()
        presentation.register(first, depth: 1)
        presentation.register(second, depth: 1)
        presentation.register(first, depth: 1)
        #expect(presentation.frontmostSurface == second)
        presentation.unregister(second)
        #expect(presentation.frontmostSurface == first)
        #expect(SnackbarPresentation().frontmostSurface == nil)
    }

    @Test
    func handoffPreservesIdentityAndDismissalIsIdempotent() {
        let presentation = SnackbarPresentation()
        let first = UUID()
        let next = UUID()
        let sheet = UUID()

        #expect(presentation.show(first))
        let visibleDeadline = presentation.deadline
        presentation.register(sheet, depth: 1)
        #expect(!presentation.show(first))
        #expect(presentation.messageID == first)
        #expect(presentation.deadline == visibleDeadline)
        #expect(presentation.beginDismissal(first))
        let exitDeadline = presentation.deadline
        presentation.unregister(sheet)
        #expect(presentation.isExiting)
        #expect(!presentation.beginDismissal(first))
        #expect(presentation.deadline == exitDeadline)
        #expect(presentation.completeDismissal(first))
        #expect(presentation.show(next))
        #expect(!presentation.beginDismissal(first))
        #expect(!presentation.completeDismissal(first))
        #expect(presentation.messageID == next)
        #expect(!presentation.isExiting)
    }
}
