#!/bin/sh
#
# Add one or more RSS feeds to the app on a connected Android device/emulator.
#
# Fires the same ACTION_SEND intent the "share to FeedFlow" flow uses, so the
# feed is fetched and stored exactly like a real share.
#
# Usage:
#   .scripts/add-android-feed.sh <url> [<url> ...]
#
# Options:
#   -s, --device <serial>  Target device (defaults to the only connected one)
#   --app-id <id>          App id to target (default: com.prof18.feedflow.debug)
#   --delay <seconds>      Wait between feeds (default: 6)
#
# Examples:
#   .scripts/add-android-feed.sh https://news.ycombinator.com/rss
#   .scripts/add-android-feed.sh -s emulator-5554 https://xkcd.com/rss.xml

set -e

APP_ID="com.prof18.feedflow.debug"
DEVICE=""
DELAY=6

usage() {
    echo "Usage: $0 [-s <serial>] [--app-id <id>] [--delay <seconds>] <url> [<url> ...]"
}

FEEDS=""
while [ $# -gt 0 ]; do
    case "$1" in
        -s|--device)
            [ $# -ge 2 ] || { echo "Error: $1 needs a value" >&2; exit 1; }
            DEVICE="$2"
            shift 2
            ;;
        --app-id)
            [ $# -ge 2 ] || { echo "Error: $1 needs a value" >&2; exit 1; }
            APP_ID="$2"
            shift 2
            ;;
        --delay)
            [ $# -ge 2 ] || { echo "Error: $1 needs a value" >&2; exit 1; }
            DELAY="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            echo "Error: unknown option '$1'" >&2
            usage >&2
            exit 1
            ;;
        *)
            FEEDS="$FEEDS $1"
            shift
            ;;
    esac
done

if [ -z "$FEEDS" ]; then
    usage >&2
    exit 1
fi

for FEED in $FEEDS; do
    case "$FEED" in
        http://*|https://*) ;;
        *)
            echo "Error: '$FEED' is not an http(s) URL." >&2
            exit 1
            ;;
    esac
done

command -v adb >/dev/null 2>&1 || {
    echo "Error: adb not found on PATH." >&2
    exit 1
}

if [ -z "$DEVICE" ]; then
    DEVICE_COUNT=$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "Error: no device connected. Start one with 'android emulator start Resizable_Experimental'." >&2
        exit 1
    fi
    if [ "$DEVICE_COUNT" -gt 1 ]; then
        echo "Error: multiple devices connected, pick one with -s <serial>:" >&2
        adb devices | awk 'NR>1 && $2=="device" {print "  " $1}' >&2
        exit 1
    fi
    DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
fi

ADB="adb -s $DEVICE"

$ADB shell pm path "$APP_ID" >/dev/null 2>&1 || {
    echo "Error: $APP_ID is not installed on $DEVICE. Run .scripts/run-android.sh first." >&2
    exit 1
}

FIRST=1
for FEED in $FEEDS; do
    # The activity fetches the feed, shows the result for 2s and finishes, so
    # give it room to settle before firing the next one.
    [ $FIRST -eq 1 ] || sleep "$DELAY"
    FIRST=0

    echo "Adding $FEED"
    $ADB shell am start \
        -a android.intent.action.SEND \
        -t "text/plain" \
        --es android.intent.extra.TEXT "$FEED" \
        -n "$APP_ID/com.prof18.feedflow.android.addfeed.AddFeedExtensionActivity" \
        >/dev/null
done

echo "Done. Check the device for the result of each add."
