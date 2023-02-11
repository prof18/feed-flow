package com.prof18.feedflow

import android.app.Application
import android.content.Context
import org.koin.dsl.module

class FeedFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin(
            module {
                single<Context> { this@FeedFlowApp }
            } + appModule,
        )
    }
}