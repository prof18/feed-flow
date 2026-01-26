# GReader Mock Fixtures

This directory contains JSON response fixtures for testing the GReader client.

## Structure

Place your captured API responses in this directory with descriptive filenames:

### Authentication
- `login_success.txt` - Successful login response
- `login_unauthorized.json` - Failed login response
- `token.txt` - Token endpoint response

### Subscriptions
- `subscriptions_list.json` - List of user subscriptions
- `subscription_quickadd_success.json` - Successful feed addition
- `subscription_edit_success.json` - Successful subscription edit

### Streams
- `stream_items_ids.json` - Stream item IDs response
- `stream_contents_reading_list.json` - Reading list contents
- `stream_contents_starred.json` - Starred items contents
- `stream_item_contents.json` - Specific item contents

### Tags
- `edit_tag_success.json` - Tag edit success response
- `rename_tag_success.json` - Tag rename success response

## Adding New Fixtures

1. Run the app with debug logging enabled
2. Capture the JSON response from the console logs
3. Save it to this directory with a descriptive name
4. Reference it in your test using `addMockResponse(responseFile = "filename.json")`

See docs/MOCK_SETUP_GUIDE.md for detailed instructions on extracting responses from logs.
