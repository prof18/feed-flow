package com.prof18.feedflow.shared.domain.parser

internal data class ReaderModeParseResult(
    val content: String,
    val title: String?,
    val siteName: String?,
    val timings: ReaderModeParseTimings?,
)

internal data class ReaderModeParseTimings(
    val totalMillis: Long?,
    val domMillis: Long?,
    val cleanupMillis: Long?,
    val defuddleMillis: Long?,
    val inputChars: Long?,
    val defuddleProfiles: List<ReaderModeDefuddleProfile>,
)

internal data class ReaderModeDefuddleProfile(
    val elapsedMillis: Long?,
    val options: String?,
    val wordCount: Long?,
    val contentChars: Long?,
    val steps: String?,
)
