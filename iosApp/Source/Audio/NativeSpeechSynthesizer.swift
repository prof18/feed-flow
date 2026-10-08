import AVFoundation
import Darwin
import Foundation

@MainActor
final class NativeSpeechSynthesizer: SpeechSynthesizerDriving {
    private let synthesizer = AVSpeechSynthesizer()
    private var continuation: CheckedContinuation<Bool, Error>?
    private var receiveBuffer: (@MainActor (AVAudioPCMBuffer) throws -> Void)?
    private var activeUtteranceID: UUID?
    private var utterance = AVSpeechUtterance(string: "")
    private var finished = false

    func synthesize(_ text: String, receive: @escaping @MainActor (AVAudioPCMBuffer) throws -> Void) async throws -> Bool {
        let utteranceID = UUID()
        return try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                self.continuation = continuation
                receiveBuffer = receive
                activeUtteranceID = utteranceID
                finished = false
                utterance = AVSpeechUtterance(string: text)
                synthesizer.write(utterance) { [weak self] audioBuffer in
                    guard let pcmBuffer = audioBuffer as? AVAudioPCMBuffer else { return }
                    let isEnd = pcmBuffer.frameLength == 0
                    let ownedBuffer: AVAudioPCMBuffer?
                    if isEnd {
                        ownedBuffer = nil
                    } else {
                        guard let copy = Self.copyBuffer(pcmBuffer) else { return }
                        ownedBuffer = copy
                    }
                    DispatchQueue.main.async { [weak self] in
                        guard let self, self.activeUtteranceID == utteranceID, !self.finished else { return }
                        if isEnd {
                            self.finished = true
                            self.receiveBuffer = nil
                            self.continuation?.resume(returning: true)
                            self.continuation = nil
                        } else {
                            guard let ownedBuffer else { return }
                            do { try self.receiveBuffer?(ownedBuffer) } catch {
                                self.finished = true
                                self.synthesizer.stopSpeaking(at: .immediate)
                                self.continuation?.resume(throwing: error)
                                self.continuation = nil
                                self.receiveBuffer = nil
                            }
                        }
                    }
                }
            }
        } onCancel: {
            DispatchQueue.main.async { [weak self] in
                guard let self, self.activeUtteranceID == utteranceID else { return }
                self.stop()
            }
        }
    }

    func stop() {
        finished = true
        activeUtteranceID = nil
        synthesizer.stopSpeaking(at: .immediate)
        continuation?.resume(throwing: CancellationError())
        continuation = nil
        receiveBuffer = nil
    }

    nonisolated private static func copyBuffer(_ source: AVAudioPCMBuffer) -> AVAudioPCMBuffer? {
        guard let copy = AVAudioPCMBuffer(pcmFormat: source.format, frameCapacity: source.frameLength) else { return nil }
        copy.frameLength = source.frameLength
        let sourceList = UnsafeMutablePointer(mutating: source.audioBufferList)
        let sourceBuffers = UnsafeMutableAudioBufferListPointer(sourceList)
        let destinationBuffers = UnsafeMutableAudioBufferListPointer(copy.mutableAudioBufferList)
        guard sourceBuffers.count == destinationBuffers.count else { return nil }
        for index in sourceBuffers.indices {
            guard let sourceData = sourceBuffers[index].mData,
                  let destinationData = destinationBuffers[index].mData,
                  destinationBuffers[index].mDataByteSize >= sourceBuffers[index].mDataByteSize else { return nil }
            memcpy(destinationData, sourceData, Int(sourceBuffers[index].mDataByteSize))
        }
        return copy
    }
}
