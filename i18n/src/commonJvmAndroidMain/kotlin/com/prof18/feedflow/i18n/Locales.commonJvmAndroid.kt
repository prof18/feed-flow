package com.prof18.feedflow.i18n

actual fun String.format(vararg args: Any): String =
    java.lang.String.format(this, *args)
