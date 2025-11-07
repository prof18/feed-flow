package com.prof18.feedflow.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.prof18.feedflow",
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()

            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)

            device.waitForIdle()
        }
    }
}
