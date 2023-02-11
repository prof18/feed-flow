package com.prof18.feedflow

val opml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <opml version="1.0">
        <head>
            <title>Marco subscriptions in feedly Cloud</title>
        </head>
        <body>
            <outline text="Tech" title="Tech">
                <outline type="rss" text="Hacker News" title="Hacker News" xmlUrl="https://news.ycombinator.com/rss" htmlUrl="https://news.ycombinator.com/"/>
                <outline type="rss" text="Android Police - Feed" title="Android Police - Feed" xmlUrl="http://www.androidpolice.com/feed/" htmlUrl="https://www.androidpolice.com"/>
                <outline type="rss" text="TechCrunch" title="TechCrunch" xmlUrl="https://techcrunch.com/feed/" htmlUrl="https://techcrunch.com/"/>
            </outline>
            <outline text="Basket" title="Basket">
                <outline type="rss" text="Pianeta Basket" title="Pianeta Basket" xmlUrl="http://www.pianetabasket.com/rss/" htmlUrl="https://www.pianetabasket.com"/>
                <outline type="rss" text="Overtime" title="Overtime" xmlUrl="https://www.overtimebasket.com/feed/" htmlUrl="https://www.overtimebasket.com"/>
            </outline>
            <outline text="News" title="News">
                <outline type="rss" text="Il Post" title="Il Post" xmlUrl="http://feeds.ilpost.it/ilpost" htmlUrl="https://www.ilpost.it"/>
            </outline>
        </body>
    </opml>
""".trimIndent()