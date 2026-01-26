# Feedbin Mock Fixtures

This directory contains JSON response fixtures for testing the Feedbin client.

## Structure

Place your captured API responses in this directory with descriptive filenames:

### Authentication
- `auth_success.json` - Successful authentication response

### Subscriptions
- `subscriptions_list.json` - List of user subscriptions
- `subscription_create.json` - Create subscription response
- `subscription_update.json` - Update subscription response

### Entries
- `entries_list.json` - List of entries
- `entries_page.json` - Paginated entries response
- `unread_entries.json` - List of unread entry IDs
- `starred_entries.json` - List of starred entry IDs

### Taggings
- `taggings_list.json` - List of taggings
- `tagging_create.json` - Create tagging response
- `tagging_delete.json` - Delete tagging response
- `tag_rename.json` - Rename tag response

### Icons
- `icons_list.json` - List of feed icons

## Adding New Fixtures

1. Run the app with debug logging enabled
2. Capture the JSON response from the console logs
3. Save it to this directory with a descriptive name
4. Reference it in your test using `addMockResponse(responseFile = "filename.json")`

See docs/MOCK_SETUP_GUIDE.md for detailed instructions on extracting responses from logs.
