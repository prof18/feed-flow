# Bazqux Mock Fixtures

This directory contains JSON response fixtures for testing the GReader client against Bazqux.

## Fixtures

### Authentication
- `login_success.txt` - Successful login response (Bazqux format: SID=unused, LSID=unused, Auth=token)
- `token.txt` - Token endpoint response

### Subscriptions
- `subscriptions_list.json` - List of user subscriptions with categories

### Streams
- `stream_items_ids_unread.json` - First page of unread item IDs
- `stream_items_ids_unread_page2.json` - Second page of unread item IDs (with continuation)
- `stream_items_ids_starred.json` - Starred item IDs
- `stream_contents_page1.json` - First page of stream contents (POST method)
- `stream_contents_starred.json` - Starred items contents (POST method)

## Usage

```kotlin
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.test.greader.createMockGReaderHttpClient
import com.prof18.feedflow.feedsync.test.greader.configureBazquxMocks

val mockHttpClient = createMockGReaderHttpClient(
    provider = SyncAccounts.BAZQUX,
    baseURL = "https://bazqux.com/",
) {
    configureBazquxMocks()
}
```

Or with DI:

```kotlin
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureBazquxMocks

override fun getTestModules() = super.getTestModules() + getFeedSyncTestModules(
    gReaderProvider = SyncAccounts.BAZQUX,
    gReaderBaseURL = "https://bazqux.com/",
    gReaderConfig = {
        configureBazquxMocks()
    }
)
```

## Notes

- These fixtures are based on actual Bazqux API responses
- Bazqux uses POST method for `/reader/api/0/stream/items/contents` (unlike FreshRSS which uses GET)
- Stream contents responses include continuation tokens for pagination
- Bazqux login format: `SID=unused`, `LSID=unused`, `Auth=<token>`
