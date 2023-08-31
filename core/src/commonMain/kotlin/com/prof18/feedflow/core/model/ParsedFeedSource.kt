package com.prof18.feedflow.core.model

data class ParsedFeedSource(
    val url: String,
    val title: String,
    val category: String?,
) {
    data class Builder(
        private var url: String? = null,
        private var title: String? = null,
        private var category: String? = null,
    ) {
        fun url(url: String?) = apply { this.url = url?.replace("http://", "https://") }
        fun title(title: String?) = apply { this.title = title }
        fun titleIfNull(title: String?) = apply {
            if (this.title == null) {
                this.title = title
            }
        }
        fun category(category: String?) = apply { this.category = category }

        fun build(): ParsedFeedSource? {
            if (url == null || title == null) {
                return null
            }
            return ParsedFeedSource(
                url = url!!,
                title = title!!,
                category = category,
            )
        }
    }
}
