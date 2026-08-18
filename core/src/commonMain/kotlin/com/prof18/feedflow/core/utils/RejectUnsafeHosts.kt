package com.prof18.feedflow.core.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin

/**
 * Rejects requests whose host would crash the Darwin engine, see [HttpHostValidator].
 *
 * This intercepts [HttpSend] instead of the request pipeline because a redirect never goes through
 * the latter: `HttpRedirect` rewrites the URL from the `Location` header and re-enters the send
 * chain, so the host that reaches the engine is not the one the caller asked for. Interceptors
 * registered later run closer to the engine, so this has to be installed on an already built client
 * to sit after `HttpRedirect` and see every hop.
 */
fun HttpClient.rejectUnsafeHosts(): HttpClient = apply {
    plugin(HttpSend).intercept { request ->
        val host = request.url.host
        require(HttpHostValidator.isSafeForHttpClient(host)) {
            "Unsupported host for HTTP client: $host"
        }
        execute(request)
    }
}
