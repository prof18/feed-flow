package com.prof18.feedflow.shared.test

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

fun unexpectedRequestHttpClient(): HttpClient =
    HttpClient(MockEngine) {
        engine {
            addHandler {
                error("Unexpected HTTP request")
            }
        }
    }
