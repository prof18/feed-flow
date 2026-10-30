package com.prof18.feedflow.shared.utils

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class StaticHeadersInterceptor(
    private val headers: Map<String, String>,
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request().newBuilder().apply {
            headers.forEach { (key, value) -> header(key, value) }
        }.build()
        return chain.proceed(request)
    }
}
