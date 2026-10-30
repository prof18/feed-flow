package com.prof18.feedflow.shared.test.koin

import com.prof18.feedflow.shared.domain.recovery.CapturedFeedParser
import com.prof18.feedflow.shared.domain.recovery.WebsiteSessionHttp
import com.prof18.feedflow.shared.test.fakes.FakeWebsiteSessionHttp
import com.prof18.rssparser.model.RssChannel
import org.koin.core.module.Module
import org.koin.dsl.module

object RecoveryTestModules {
    fun enabled(fake: FakeWebsiteSessionHttp, channel: RssChannel?): Module = module {
        single<WebsiteSessionHttp> { fake }
        single { CapturedFeedParser { channel ?: error("no fixture channel") } }
    }
}
