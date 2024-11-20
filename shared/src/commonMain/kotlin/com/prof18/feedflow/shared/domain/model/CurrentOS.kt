package com.prof18.feedflow.shared.domain.model

internal sealed interface CurrentOS {
    data object Android : CurrentOS
    data object Ios : CurrentOS
    sealed class Desktop : CurrentOS {
        data object Windows : Desktop()
        data object Mac : Desktop()
        data object Linux : Desktop()
    }
}
