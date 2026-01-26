package com.prof18.feedflow.feedsync.test

actual fun loadFixture(filename: String): String {
    // Try multiple classloader approaches
    val resource = LoadFixture::class.java.classLoader?.getResource("fixtures/$filename")
        ?: Thread.currentThread().contextClassLoader?.getResource("fixtures/$filename")
        ?: LoadFixture::class.java.getResource("/fixtures/$filename")
        ?: throw IllegalArgumentException("Fixture file not found: fixtures/$filename")

    return resource.readText()
}

private object LoadFixture
