package com.prof18.feedflow.android

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

object PlayReviewManager {
    fun triggerReviewFlow(
        activity: Activity,
        onReviewDone: () -> Unit,
    ) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    onReviewDone()
                }
            }
        }
    }
}
