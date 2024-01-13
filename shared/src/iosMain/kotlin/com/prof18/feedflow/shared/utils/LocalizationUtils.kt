package com.prof18.feedflow.shared.utils

import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.format

object LocalizationUtils {
    fun formatString(resource: StringResource, args: List<Any>): String {
        return resource.format(args).localized()
    }
}
