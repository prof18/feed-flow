import Foundation

enum AudioTestWaveFile {
    static func write(to url: URL) throws {
        let bytes = 96000
        var data = Data("RIFF".utf8)
        append(UInt32(bytes + 36), to: &data)
        data.append(Data("WAVEfmt ".utf8))
        append(UInt32(16), to: &data)
        append(UInt16(1), to: &data)
        append(UInt16(1), to: &data)
        append(UInt32(8000), to: &data)
        append(UInt32(16000), to: &data)
        append(UInt16(2), to: &data)
        append(UInt16(16), to: &data)
        data.append(Data("data".utf8))
        append(UInt32(bytes), to: &data)
        data.append(Data(repeating: 0, count: bytes))
        try data.write(to: url)
    }

    private static func append<T: FixedWidthInteger>(_ value: T, to data: inout Data) {
        var littleEndian = value.littleEndian
        withUnsafeBytes(of: &littleEndian) { data.append(contentsOf: $0) }
    }
}
