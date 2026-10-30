import XCTest

final class AudioPlaybackProgressTests: XCTestCase {
    func testUnknownAndInvalidMediaTimesDoNotPersistInvalidPositions() {
        XCTAssertEqual(AudioPlaybackProgress.milliseconds(.nan), 0)
        XCTAssertEqual(AudioPlaybackProgress.milliseconds(.infinity), 0)
        XCTAssertEqual(AudioPlaybackProgress.milliseconds(-5), 0)
        XCTAssertEqual(AudioPlaybackProgress.milliseconds(75.123), 75123)
        XCTAssertGreaterThan(AudioPlaybackProgress.milliseconds(.greatestFiniteMagnitude), 0)
    }

    func testSeekClampsToKnownDurationAndAllowsSavedPositionBeforeDurationLoads() {
        XCTAssertEqual(AudioPlaybackProgress.seekPosition(500, duration: 100), 100)
        XCTAssertEqual(AudioPlaybackProgress.seekPosition(-10, duration: 100), 0)
        XCTAssertEqual(AudioPlaybackProgress.seekPosition(75, duration: 0), 75)
        XCTAssertEqual(AudioPlaybackProgress.seekPosition(75, duration: .nan), 75)
        XCTAssertEqual(AudioPlaybackProgress.seekPosition(.infinity, duration: 100), 0)
    }
}
