# Miniflux Mock Fixtures

This directory contains JSON response fixtures for testing the GReader client against Miniflux.

## Fixtures

### Authentication
- `login_success.txt` - Successful login response (Miniflux format: SID=username/hash, LSID=username/hash, Auth=username/hash)
- `token.txt` - Token endpoint response

### Subscriptions
- `subscriptions_list.json` - List of user subscriptions with categories

### Streams
- `stream_items_ids_unread.json` - First page of unread item IDs
- `stream_items_ids_unread_page2.json` - Second page of unread item IDs (with continuation)
- `stream_items_ids_starred.json` - Starred item IDs
- `stream_contents_page1.json` - First page of stream contents (POST method)
- `stream_contents_page2.json` - Second page of stream contents (POST method)
- `stream_contents_starred.json` - Starred items contents (POST method)

## Usage

```kotlin
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.test.greader.createMockGReaderHttpClient
import com.prof18.feedflow.feedsync.test.greader.configureMinifluxMocks

val mockHttpClient = createMockGReaderHttpClient(
    provider = SyncAccounts.MINIFLUX,
    baseURL = "https://miniflux.example.com/",
) {
    configureMinifluxMocks()
}
```

Or with DI:

```kotlin
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureMinifluxMocks

override fun getTestModules() = super.getTestModules() + getFeedSyncTestModules(
    gReaderProvider = SyncAccounts.MINIFLUX,
    gReaderBaseURL = "https://miniflux.example.com/",
    gReaderConfig = {
        configureMinifluxMocks()
    }
)
```

## Notes

- These fixtures are based on actual Miniflux API responses
- Miniflux uses POST method for `/reader/api/0/stream/items/contents` (unlike FreshRSS which uses GET)
- Stream contents responses include continuation tokens for pagination
- Miniflux login format: `SID=username/hash`, `LSID=username/hash`, `Auth=username/hash`
