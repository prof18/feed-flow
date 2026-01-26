# FreshRSS Mock Fixtures

This directory contains JSON response fixtures for testing the GReader client against FreshRSS.

## Fixtures

### Authentication
- `login_success.txt` - Successful login response (FreshRSS format: SID=username/hash, LSID=null, Auth=username/hash)
- `token.txt` - Token endpoint response

### Subscriptions
- `subscriptions_list.json` - List of user subscriptions with categories

### Streams
- `stream_contents_reading_list_page1.json` - First page of reading list contents
- `stream_contents_reading_list_page2.json` - Second page of reading list contents (with continuation)
- `stream_contents_starred.json` - Starred items contents
- `stream_items_ids_unread.json` - Unread item IDs
- `stream_items_ids_starred.json` - Starred item IDs

## Usage

```kotlin
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.test.greader.createMockGReaderHttpClient
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocks

val mockHttpClient = createMockGReaderHttpClient(
    provider = SyncAccounts.FRESH_RSS,
    baseURL = "https://freshrss.example.com/api/greader.php/",
) {
    configureFreshRssMocks()
}
```

Or with DI:

```kotlin
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.test.di.getFeedSyncTestModules
import com.prof18.feedflow.feedsync.test.greader.configureFreshRssMocks

override fun getTestModules() = super.getTestModules() + getFeedSyncTestModules(
    gReaderProvider = SyncAccounts.FRESH_RSS,
    gReaderBaseURL = "https://freshrss.example.com/api/greader.php/",
    gReaderConfig = {
        configureFreshRssMocks()
    }
)
```

## Notes

- These fixtures are based on actual FreshRSS API responses
- FreshRSS login format differs from other providers: `LSID=null` instead of a value
- Stream contents responses include continuation tokens for pagination
