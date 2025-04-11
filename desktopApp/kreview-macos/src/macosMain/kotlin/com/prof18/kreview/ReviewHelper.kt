package com.prof18.kreview

import com.prof18.jni.JNIEnvVar
import com.prof18.jni.jclass
import kotlinx.cinterop.CPointer
import platform.StoreKit.SKStoreReviewController

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_desktop_macosreview_MacosReviewBridge_triggerAppStoreReview")
fun triggerAppStoreReview(env: CPointer<JNIEnvVar>, clazz: jclass) {
    SKStoreReviewController.requestReview()
}
