package com.prof18.feedflow.shared.domain.model

internal sealed interface CurrentOS {
    data object Android : CurrentOS
    data object Ios : CurrentOS
    data object Desktop : CurrentOS
}
