package com.prof18.feedflow.shared.test

import io.kotest.property.Arb
import io.kotest.property.RandomSource

fun <T> Arb<T>.sampleValue(): T = sample(RandomSource.default()).value
