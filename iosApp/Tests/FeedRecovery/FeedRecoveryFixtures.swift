import Foundation

enum FeedRecoveryFixtures {
    static let rss = Data("""
    <?xml version="1.0" encoding="UTF-8"?>
    <rss version="2.0"><channel><title>Recovery fixture</title>
    <link>https://example.com</link><description>Synthetic feed</description>
    <item><title>Fixture article</title><link>https://example.com/article</link></item>
    </channel></rss>
    """.utf8)
}
