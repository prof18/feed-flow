package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.FeedSourceCategory
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.string
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object CategoryGenerator {
    val categoryArb = arbitrary {
        FeedSourceCategory(
            id = Uuid.random().toString(),
            title = Arb.string(5..30).bind(),
        )
    }
}
