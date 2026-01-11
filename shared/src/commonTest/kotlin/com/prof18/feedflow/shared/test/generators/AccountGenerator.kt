package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.SyncAccounts
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum

object AccountGenerator {
    val syncAccountArb = Arb.enum<SyncAccounts>()
}
