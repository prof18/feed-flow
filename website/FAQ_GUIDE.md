# FAQ Management Guide

This guide explains how to manage the FAQ section on the FeedFlow website.

## Overview

The FAQ section is a standalone page built with Hugo that displays frequently asked questions. It's accessible at `https://www.feedflow.dev/faq/` and is integrated into all FeedFlow apps (Android, iOS, and Desktop).

## Structure

```
website/
├── content/
│   └── en/
│       └── faq/
│           ├── _index.md              # FAQ page header
│           ├── 01-no-feed-content.md  # Individual FAQ item
│           ├── 02-import-feeds.md     # Individual FAQ item
│           └── ...
└── themes/apsho/layouts/
    └── faq/
        └── list.html                  # FAQ page template
```

## Adding a New FAQ

To add a new FAQ question:

1. Create a new markdown file in `website/content/en/faq/` with a numbered prefix:
   ```bash
   website/content/en/faq/06-my-new-question.md
   ```

2. Add the following front matter and content:
   ```markdown
   ---
   title: "Your question here?"
   weight: 6
   ---

   Your answer here. You can use:
   - **Bold text**
   - *Italic text*
   - Markdown lists
   - Links: [Link text](https://example.com)
   - Code blocks
   - And more!
   ```

3. The `weight` field controls the order (lower numbers appear first).

4. The FAQ will automatically appear on the page once Hugo rebuilds the site.

## Features

The FAQ page includes:

- **Search functionality**: Users can search through questions and answers
- **Accordion design**: Questions expand/collapse when clicked
- **Mobile responsive**: Works great on all screen sizes
- **Beautiful gradient background**: Eye-catching purple gradient
- **Automatic highlighting**: Search results are automatically expanded

## Adding Translations

The FAQ supports multiple languages through Hugo's multilingual features:

1. Create language-specific content directories:
   ```bash
   mkdir -p website/content/it/faq  # Italian
   mkdir -p website/content/de/faq  # German
   ```

2. Copy the FAQ files and translate them:
   ```bash
   cp website/content/en/faq/*.md website/content/it/faq/
   ```

3. Translate the content in each file

4. Add the language configuration in `website/config.toml`:
   ```toml
   [languages.it]
     languageCode = "it"
     languageName = "Italiano"
     weight = 2
     title = "FeedFlow | Lettore RSS"
   ```

## Customizing the Design

The FAQ page styling is embedded in `website/themes/apsho/layouts/faq/list.html`. You can customize:

- **Colors**: Search for color values in the `<style>` section
- **Spacing**: Adjust padding and margin values
- **Typography**: Modify font sizes and weights
- **Animations**: Change transition speeds

## App Integration

The FAQ is integrated into all FeedFlow apps:

- **Android**: About screen → FAQ button (opens in browser/custom tab)
- **iOS/macOS**: About screen → FAQ button (opens in Safari)
- **Desktop**: About screen → FAQ button (opens in default browser)

This allows you to update the FAQ content without releasing new app versions!

## Testing Locally

To test the FAQ page locally:

1. Install Hugo: https://gohugo.io/installation/
2. Navigate to the website directory:
   ```bash
   cd website
   ```
3. Run Hugo server:
   ```bash
   hugo server
   ```
4. Open http://localhost:1313/faq/ in your browser

## Tips

- **Keep answers concise**: Users want quick answers
- **Use formatting**: Bold important keywords, use lists for steps
- **Add context**: Explain why something works the way it does
- **Include examples**: Show users exactly what to do
- **Update regularly**: Review and update FAQs based on user feedback
- **Use clear titles**: Make questions searchable and easy to understand

## Common Tasks

### Reordering FAQs

Change the `weight` value in the front matter. Lower weights appear first.

### Removing a FAQ

Simply delete the markdown file. The FAQ will disappear on next build.

### Changing the FAQ page description

Edit `website/content/en/faq/_index.md`

### Adding images

Place images in `website/static/images/faq/` and reference them:
```markdown
![Description](/images/faq/my-image.png)
```
