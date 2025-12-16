package com.prof18.feedflow.desktop.utils

import cafe.adriel.voyager.core.screen.Screen
import java.util.*

internal fun Screen.generateUniqueKey(): String =
    "${this::class.qualifiedName}${UUID.randomUUID()}"
