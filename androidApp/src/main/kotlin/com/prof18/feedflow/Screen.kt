package com.prof18.feedflow

sealed class Screen(val name: String) {
    object Home: Screen("home")
    object ImportFeed: Screen("import_feed")
    object Settings: Screen("setting")
    object AddFeed: Screen("add_feed")
    object FeedList: Screen("feed_list")
}


fun main() {
    var imageUrlFromContent: String? = null
    val content = "<img src='https://images.news18.com/ibnkhabar/uploads/2021/01/who-team-1.jpg' height='50' width='76' />कोरोना वायरस की उत्पत्ति को लेकर विश्व स्वास्थ्य संगठन (WHO) के विशेषज्ञ शुक्रवार को चीन के वुहान (Wuhan) में अधिकारियों के साथ बैठक करेंगे."
    val imgRegex = Regex(pattern = "(<img .*?>)")

    imgRegex.find(content)?.value?.let { imgString ->
        val urlRegex = Regex(pattern = "src\\s*=\\s*([\"'])(.+?)([\"'])")
        urlRegex.find(imgString)?.groupValues?.getOrNull(2)?.trim().let { imgUrl ->
            imageUrlFromContent = imgUrl
        }
    }

    print("")
}