package com.prof18.feedflow.android.feedverification

import android.Manifest
import com.prof18.feedflow.android.FeedFlowApp
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric does not grant manifest permissions, and RevenueCat refuses to start without INTERNET when
 * the build carries a RevenueCat key.
 */
class InternetGrantedFeedFlowApp : FeedFlowApp() {
    override fun onCreate() {
        shadowOf(this).grantPermissions(Manifest.permission.INTERNET)
        super.onCreate()
    }
}
