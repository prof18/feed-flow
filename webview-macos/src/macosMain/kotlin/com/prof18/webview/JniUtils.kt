package com.prof18.webview

import com.prof18.jni.JNIEnvVar
import com.prof18.jni.jstring
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
internal fun jstring.toKotlinString(env: CPointer<JNIEnvVar>): String? {
    val jniEnv = env.pointed.pointed ?: return null
    val getStringUTFChars = jniEnv.GetStringUTFChars ?: return null
    val releaseStringUTFChars = jniEnv.ReleaseStringUTFChars ?: return null

    val chars = getStringUTFChars.invoke(env, this, null) ?: return null
    val result = chars.toKString()
    releaseStringUTFChars.invoke(env, this, chars)

    return result
}
