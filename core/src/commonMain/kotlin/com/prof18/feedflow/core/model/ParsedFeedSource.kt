package com.prof18.feedflow.core.model

data class ParsedFeedSource(
    val id: String,
    val url: String,
    val title: String,
    val category: FeedSourceCategory?,
    val logoUrl: String?,
) {
    data class Builder(
        private var url: String? = null,
        private var title: String? = null,
        private var category: String? = null,
        private var logoUrl: String? = null,
    ) {
        fun url(url: String?) = apply { this.url = url?.replace("http://", "https://") }
        fun title(title: String?) = apply { this.title = title }
        fun titleIfNull(title: String?) = apply {
            if (this.title == null) {
                this.title = title
            }
        }

        fun category(category: String?) = apply { this.category = category }

        fun logoUrl(url: String?) = apply { this.logoUrl = url }

        fun build(): ParsedFeedSource? {
            if (url == null || title == null) {
                return null
            }
            return ParsedFeedSource(
                id = url.hashCode().toString(),
                url = url!!,
                title = title!!,
                category = category?.let { categoryName ->
                    FeedSourceCategory(
                        id = categoryName.hashCode().toString(),
                        title = categoryName,
                    )
                },
                logoUrl = logoUrl,
            )
        }
    }
}
