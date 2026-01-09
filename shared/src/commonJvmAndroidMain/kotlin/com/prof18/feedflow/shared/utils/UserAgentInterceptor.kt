package com.prof18.feedflow.shared.utils

import com.prof18.feedflow.core.utils.FEEDFLOW_USER_AGENT
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", FEEDFLOW_USER_AGENT)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
