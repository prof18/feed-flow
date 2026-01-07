# Feed Sync Error Codes

This document maps sync error scenarios to specific error codes for better debugging and user communication.

## Error Code Format
Format: `[Category][Number]` (e.g., `SU1`, `SD2`, `DB3`)

Categories:
- **SU** = Sync Upload
- **SD** = Sync Download
- **SF** = Sync Feeds
- **SI** = Sync iCloud
- **DB** = Database
- **FS** = Feed Sync (FreshRSS)
- **FF** = Feed Fetch

## Internal Debug Mapping (For Development)

| Error Code | Technical Description | Location/Method | Platform | Underlying Cause |
|------------|----------------------|-----------------|----------|------------------|
| SU1 | Database file generation failed during upload | `FeedSyncAndroidWorker.kt` | Android | File system issue |
| SU2 | Dropbox upload operation failed | `FeedSyncAndroidWorker.kt`, `FeedSyncIosWorker.kt`, `FeedSyncJvmWorker.kt` | All | Network/API issue |
| SU3 | Dropbox API error (FeedFlow.DropboxErrors error 0) | `FeedSyncIosWorker.performUpload` | iOS | Dropbox SDK error |
| SD1 | Dropbox download operation failed | `FeedSyncAndroidWorker.kt`, `FeedSyncIosWorker.kt`, `FeedSyncJvmWorker.kt` | All | Network/API issue |
| SD2 | iCloud download operation failed | `FeedSyncIosWorker.kt` | iOS | iCloud API issue |
| SF1 | Feed sources synchronization failed | `FeedSyncAndroidWorker.kt`, `FeedSyncIosWorker.kt`, `FeedSyncJvmWorker.kt` | All | Sync operation failure |
| SF2 | Feed items synchronization failed | `FeedSyncAndroidWorker.kt`, `FeedSyncIosWorker.kt`, `FeedSyncJvmWorker.kt` | All | Sync operation failure |
| SI1 | iCloud service not available | `ICloudSyncViewModel.ios.kt` | iOS | iCloud base folder URL null |
| SI2 | iCloud URL not available during download | `FeedSyncIosWorker.kt` | iOS | iCloud URL null |
| SI3 | iCloud file not found (NSCocoaErrorDomain Code=260) | `FeedSyncIosWorker.iCloudDownload` | iOS | File doesn't exist |
| SI4 | iCloud copy operation failed (NSCocoaErrorDomain Code=512) | `FeedSyncIosWorker.iCloudDownload` | iOS | Timeout/operation canceled |
| SI5 | iCloud file already exists (NSCocoaErrorDomain Code=516) | `FeedSyncIosWorker.iCloudDownload` | iOS | File name conflict |
| SI6 | iCloud upload source file not found (NSCocoaErrorDomain Code=4) | `FeedSyncIosWorker.accountSpecificUpload` | iOS | Source file missing |
| DB1 | Database error during feed retrieval | `FeedStateRepository.kt` | All | Database query failure |
| DB2 | Database error during pagination | `FeedStateRepository.kt` | All | Database query failure |
| DB3 | SQLite disk I/O error | `FeedSyncIosWorker.syncFeedSources`, `FeedSyncIosWorker.syncFeedItems` | iOS | Disk I/O failure |
| DB4 | Database image malformed | `FeedSyncIosWorker.syncFeedSources`, `FeedSyncIosWorker.syncFeedItems` | iOS | Corrupted database |
| DB5 | Database connection pool closed | `FeedSyncIosWorker.syncFeedSources` | iOS | Connection pool issue |
| DB6 | Database replace operation failed | `FeedSyncIosWorker.replaceDatabase` | iOS | File replacement error |
| FS1 | Failed to delete feed source | `FeedSourcesRepository.kt` | All | FreshRSS API error |
| FS2 | Failed to edit feed source name | `FeedSourcesRepository.kt` | All | FreshRSS API error |
| FS3 | Failed to mark items as read | `FeedActionsRepository.kt` | All | FreshRSS API error |
| FS4 | Failed to mark all feeds as read | `FeedActionsRepository.kt` | All | FreshRSS API error |
| FS5 | Failed to update bookmark status | `FeedActionsRepository.kt` | All | FreshRSS API error |
| FS6 | Failed to update read status | `FeedActionsRepository.kt` | All | FreshRSS API error |
| FS7 | Failed to create category | `FeedCategoryRepository.kt` | All | FreshRSS API error |
| FS8 | Failed to fetch feed sources and categories | `FeedCategoryRepository.kt` | All | FreshRSS API error |
| FS9 | Failed to edit category name | `FeedCategoryRepository.kt` | All | FreshRSS API error |
| FS10 | Failed to sync feeds | `FeedFetcherRepository.kt` | All | FreshRSS API error |
| FF1 | Failed to fetch RSS feed | `FeedFetcherRepository.kt` | All | RSS source unavailable |
