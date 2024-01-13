package com.prof18.feedflow.shared

import org.xmlpull.v1.XmlPullParser

internal fun XmlPullParser.contains(key: String): Boolean {
    return this.name.equals(key, ignoreCase = true)
}

internal fun XmlPullParser.attributeValue(key: String): String? {
    return this.getAttributeValue(null, key)?.trim()
}
