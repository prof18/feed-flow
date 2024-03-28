package com.prof18.feedflow.i18n

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

private val PATTERNS_REGEX = "%[\\d|.]*[sdf]".toRegex()

/**
 * From: https://github.com/chrisbanes/tivi/blob/main/common/ui/resources/strings/src/iosMain/kotlin/app/tivi/common/ui/resources/String.kt#L16
 * https://youtrack.jetbrains.com/issue/KT-25506
 */
@Suppress("USELESS_CAST")
actual fun String.format(vararg args: Any): String {
    val formats = PATTERNS_REGEX.findAll(this).map { it.groupValues.first() }.toList()

    var result = this

    formats.forEachIndexed { i, format ->
        val formatted = when (val arg = args[i]) {
            is Double -> NSString.stringWithFormat(format, arg as Double)
            is Float -> NSString.stringWithFormat(format, arg as Float)
            is Int -> NSString.stringWithFormat(format, arg as Int)
            is Long -> NSString.stringWithFormat(format, arg as Long)
            else -> NSString.stringWithFormat("%@", arg)
        }
        result = result.replaceFirst(format, formatted)
    }

    // We put the string through stringWithFormat again, to remove any escaped characters
    return NSString.stringWithFormat(result, Any())
}
