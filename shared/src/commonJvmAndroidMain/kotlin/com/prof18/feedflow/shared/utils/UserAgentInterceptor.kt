package com.prof18.feedflow.shared.utils

import com.prof18.feedflow.core.utils.feedFlowUserAgent
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class UserAgentInterceptor(private val appVersion: String) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", feedFlowUserAgent(appVersion))
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
