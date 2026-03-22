package com.prof18.feedflow.core.model

enum class DescriptionLineLimit(val lines: Int) {
    THREE(lines = 3),
    FIVE(lines = 5),
    NO_LIMIT(lines = Int.MAX_VALUE),
}
