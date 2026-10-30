package com.prof18.feedflow.shared.test.fakes

import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionFailure
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionHttp
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionRequest
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionResponse

class FakeWebsiteSessionHttp : WebsiteSessionHttp {
    val requests = mutableListOf<WebsiteSessionRequest>()
    private val responses = ArrayDeque<WebsiteSessionResponse>()

    fun enqueue(vararg response: WebsiteSessionResponse) {
        responses.addAll(response)
    }

    override suspend fun fetch(request: WebsiteSessionRequest): WebsiteSessionResponse {
        requests += request
        return responses.removeFirstOrNull() ?: WebsiteSessionResponse.Failure(WebsiteSessionFailure.Transport)
    }
}
