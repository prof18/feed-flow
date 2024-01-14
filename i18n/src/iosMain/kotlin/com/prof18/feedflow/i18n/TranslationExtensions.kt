package com.prof18.feedflow.i18n

import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.ResourceStringDesc
import dev.icerock.moko.resources.desc.StringDesc

fun ResourceStringDesc.localizedWithFallback(): String {
    return localizedStringWithFallback(stringRes)
}

private const val FallbackLocale = "en"

private fun localizedStringWithFallback(stringRes: StringResource): String {
    val bundle = StringDesc.localeType.getLocaleBundle(stringRes.bundle)
    val stringInCurrentLocale = bundle.localizedStringForKey(stringRes.resourceId, null, null)
    return if (stringInCurrentLocale == stringRes.resourceId) {
        val stringInDefaultBundle = stringRes.bundle.localizedStringForKey(stringRes.resourceId, null, null)
        if (stringInDefaultBundle == stringRes.resourceId) {
            val fallbackLocale = stringRes.bundle.developmentLocalization ?: FallbackLocale
            val fallbackLocaleBundle = StringDesc.LocaleType.Custom(fallbackLocale).getLocaleBundle(bundle)
            fallbackLocaleBundle.localizedStringForKey(stringRes.resourceId, null, null)
        } else {
            stringInDefaultBundle
        }
    } else {
        stringInCurrentLocale
    }
}
