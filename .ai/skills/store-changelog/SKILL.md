---
name: store-changelog
description: Generate release notes for app stores (Android Play Store, iOS App Store, macOS App Store, Linux Flatpak). Use when the user asks for changelogs, release notes, or store descriptions based on git history.
---

# Store Changelog Generator

Generate release notes for App stores based on git history since a specified tag.

## Instructions

1. **Get the tag name**:
   - If the user provides a tag name (e.g., `1.0.0` or `v1.0.0`), use it
   - If not provided, automatically get the latest tag using: `git describe --tags --abbrev=0`
   - Inform the user which tag is being used

2. **Analyze git history**: Run `git log <tag>..HEAD --pretty=format:"%h %s%n%b" --no-merges` to get all commits since the tag, including both commit messages and descriptions.

3. **Identify changes**: Categorize commits into:
   - New features
   - Improvements
   - Bug fixes

4. **Generate platform-specific release notes** following the formats below.

## Output Formats

### Android (Play Store)
- Maximum 500 characters
- Bullet point format
- Focus on top 3-4 most impactful changes
- Example:
  ```
  • New reader mode for distraction-free reading
  • Improved sync reliability with FreshRSS
  • Fixed crash when opening large feeds
  ```

### iOS (App Store)
- User-friendly narrative format
- Highlight iOS-specific improvements (scrolling, touch interactions, mobile layout)
- Group by: New Features, Improvements, Bug Fixes
- Example:
  ```
  New Features:
  • Reader mode now provides a clean, distraction-free reading experience.

  Improvements:
  • Smoother scrolling and faster feed loading on all devices.

  Bug Fixes:
  • Fixed an issue where the app could crash when opening feeds with many articles.
  ```

### macOS (App Store)
- User-friendly narrative format
- Highlight macOS-specific improvements (desktop layout, toolbar, keyboard shortcuts, interface)
- Separate from iOS since they are different apps
- Group by: New Features, Improvements, Bug Fixes

### Linux (Flatpak metainfo.xml)
- XML format with individual `<p>` tags
- Concise, one-line descriptions per change
- Format:
  ```xml
  <release version="X.X.X" date="YYYY-MM-DD">
      <description>
          <p>Feature or fix description</p>
          <p>Another change</p>
      </description>
  </release>
  ```

## Resources

- `references/release-notes-guidelines.md`: Language, filtering, and QA rules for App Store notes.