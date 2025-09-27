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

## UI Error Codes (For Snackbar Display)

| Error Code | User Message | Category |
|------------|--------------|----------|
| SU1 | Upload failed. Check connection and try again. | Upload |
| SU2 | Upload failed. Check connection and try again. | Upload |
| SU3 | Upload failed. Check connection and try again. | Upload |
| SD1 | Download failed. Check connection and try again. | Download |
| SD2 | Download failed. Check connection and try again. | Download |
| SF1 | Sync failed. Please try again later. | Feed Sync |
| SF2 | Sync failed. Please try again later. | Feed Sync |
| SI1 | iCloud not available. Check iCloud settings. | iCloud |
| SI2 | iCloud sync failed. Check iCloud settings. | iCloud |
| SI3 | iCloud sync failed. Check iCloud settings. | iCloud |
| SI4 | iCloud sync failed. Check iCloud settings. | iCloud |
| SI5 | iCloud sync failed. Check iCloud settings. | iCloud |
| SI6 | iCloud sync failed. Check iCloud settings. | iCloud |
| DB1 | Database error. Please restart the app. | Database |
| DB2 | Database error. Please restart the app. | Database |
| DB3 | Database error. Please restart the app. | Database |
| DB4 | Database error. Please restart the app. | Database |
| DB5 | Database error. Please restart the app. | Database |
| DB6 | Database error. Please restart the app. | Database |
| FS1 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS2 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS3 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS4 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS5 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS6 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS7 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS8 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS9 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FS10 | FreshRSS sync failed. Check connection and credentials. | FreshRSS |
| FF1 | Feed unavailable. Source may be temporarily down. | Feed Fetch |

## Internal Debug Mapping (For Development)

| Error Code | Technical Description | Location/Method | Platform | Underlying Cause |
|------------|----------------------|-----------------|----------|------------------|
| SU1 | Database file generation failed during upload | `FeedSyncAndroidWorker.kt:69` | Android | File system issue |
| SU2 | Dropbox upload operation failed | `FeedSyncAndroidWorker.kt:83`, `FeedSyncIosWorker.kt:139`, `FeedSyncJvmWorker.kt:133` | All | Network/API issue |
| SU3 | Dropbox API error (FeedFlow.DropboxErrors error 0) | `FeedSyncIosWorker.performUpload` | iOS | Dropbox SDK error |
| SD1 | Dropbox download operation failed | `FeedSyncAndroidWorker.kt:103`, `FeedSyncIosWorker.kt:314`, `FeedSyncJvmWorker.kt:133` | All | Network/API issue |
| SD2 | iCloud download operation failed | `FeedSyncIosWorker.kt:349`, `FeedSyncIosWorker.kt:373` | iOS | iCloud API issue |
| SF1 | Feed sources synchronization failed | `FeedSyncAndroidWorker.kt:114`, `FeedSyncIosWorker.kt:159`, `FeedSyncJvmWorker.kt:195` | All | Sync operation failure |
| SF2 | Feed items synchronization failed | `FeedSyncAndroidWorker.kt:124`, `FeedSyncIosWorker.kt:178`, `FeedSyncJvmWorker.kt:216` | All | Sync operation failure |
| SI1 | iCloud service not available | `ICloudSyncViewModel.ios.kt:48` | iOS | iCloud base folder URL null |
| SI2 | iCloud URL not available during download | `FeedSyncIosWorker.kt:349` | iOS | iCloud URL null |
| SI3 | iCloud file not found (NSCocoaErrorDomain Code=260) | `FeedSyncIosWorker.iCloudDownload` | iOS | File doesn't exist |
| SI4 | iCloud copy operation failed (NSCocoaErrorDomain Code=512) | `FeedSyncIosWorker.iCloudDownload` | iOS | Timeout/operation canceled |
| SI5 | iCloud file already exists (NSCocoaErrorDomain Code=516) | `FeedSyncIosWorker.iCloudDownload` | iOS | File name conflict |
| SI6 | iCloud upload source file not found (NSCocoaErrorDomain Code=4) | `FeedSyncIosWorker.accountSpecificUpload` | iOS | Source file missing |
| DB1 | Database error during feed retrieval | `FeedStateRepository.kt:87` | All | Database query failure |
| DB2 | Database error during pagination | `FeedStateRepository.kt:126` | All | Database query failure |
| DB3 | SQLite disk I/O error | `FeedSyncIosWorker.syncFeedSources`, `FeedSyncIosWorker.syncFeedItems` | iOS | Disk I/O failure |
| DB4 | Database image malformed | `FeedSyncIosWorker.syncFeedSources`, `FeedSyncIosWorker.syncFeedItems` | iOS | Corrupted database |
| DB5 | Database connection pool closed | `FeedSyncIosWorker.syncFeedSources` | iOS | Connection pool issue |
| DB6 | Database replace operation failed | `FeedSyncIosWorker.replaceDatabase` | iOS | File replacement error |
| FS1 | Failed to delete feed source | `FeedSourcesRepository.kt:71` | All | FreshRSS API error |
| FS2 | Failed to edit feed source name | `FeedSourcesRepository.kt:103` | All | FreshRSS API error |
| FS3 | Failed to mark items as read | `FeedActionsRepository.kt:30` | All | FreshRSS API error |
| FS4 | Failed to mark all feeds as read | `FeedActionsRepository.kt:47` | All | FreshRSS API error |
| FS5 | Failed to update bookmark status | `FeedActionsRepository.kt:75` | All | FreshRSS API error |
| FS6 | Failed to update read status | `FeedActionsRepository.kt:93` | All | FreshRSS API error |
| FS7 | Failed to create category | `FeedCategoryRepository.kt:66` | All | FreshRSS API error |
| FS8 | Failed to fetch feed sources and categories | `FeedCategoryRepository.kt:62` | All | FreshRSS API error |
| FS9 | Failed to edit category name | `FeedCategoryRepository.kt:83` | All | FreshRSS API error |
| FS10 | Failed to sync feeds | `FeedFetcherRepository.kt:90` | All | FreshRSS API error |
| FF1 | Failed to fetch RSS feed | `FeedFetcherRepository.kt:253` | All | RSS source unavailable |
