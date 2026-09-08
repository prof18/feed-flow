package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal enum class CloudArtifactRole {
    PRODUCER,
    CONSUMER,
}

internal data class CloudArtifactExchangeConfig(
    val directory: String,
    val runId: String,
    val role: CloudArtifactRole,
    val platform: String,
    val producerPlatforms: List<String>,
)

/** Returns null only when artifact exchange has not been requested by the test runner. */
internal expect fun cloudArtifactExchangeConfig(): CloudArtifactExchangeConfig?

internal expect fun writeCloudArtifact(path: String, bytes: ByteArray)

internal expect fun readCloudArtifact(path: String): ByteArray

internal const val CLOUD_ARTIFACT_DIRECTORY_ENV = "FEEDFLOW_CLOUD_ARTIFACT_DIR"
internal const val CLOUD_ARTIFACT_ROLE_ENV = "FEEDFLOW_CLOUD_ARTIFACT_ROLE"
internal const val CLOUD_ARTIFACT_RUN_ID_ENV = "FEEDFLOW_CLOUD_ARTIFACT_RUN_ID"
internal const val CLOUD_ARTIFACT_PLATFORM_ENV = "FEEDFLOW_CLOUD_ARTIFACT_PLATFORM"
internal const val CLOUD_ARTIFACT_PRODUCERS_ENV = "FEEDFLOW_CLOUD_ARTIFACT_PRODUCERS"

internal fun cloudArtifactConfigFromEnvironment(
    root: String?,
    role: String?,
    runId: String?,
    platform: String,
    producerPlatforms: String?,
): CloudArtifactExchangeConfig? {
    if (root == null && role == null && runId == null) return null
    require(!root.isNullOrBlank()) { "$CLOUD_ARTIFACT_DIRECTORY_ENV is required when artifact exchange is configured" }
    require(!role.isNullOrBlank()) { "$CLOUD_ARTIFACT_ROLE_ENV is required when artifact exchange is configured" }
    require(!runId.isNullOrBlank()) { "$CLOUD_ARTIFACT_RUN_ID_ENV is required when artifact exchange is configured" }
    require(platform.isNotBlank()) { "$CLOUD_ARTIFACT_PLATFORM_ENV must not be blank" }
    return CloudArtifactExchangeConfig(
        directory = root,
        runId = runId,
        role = when (role.lowercase()) {
            "producer" -> CloudArtifactRole.PRODUCER
            "consumer" -> CloudArtifactRole.CONSUMER
            else -> error("$CLOUD_ARTIFACT_ROLE_ENV must be producer or consumer")
        },
        platform = platform,
        producerPlatforms = producerPlatforms?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
    )
}

internal enum class CloudArtifactExchangeOutcome {
    PRODUCED,
    CONSUMED,
}

/**
 * Exchanges the sync database produced by the real worker between separately run platforms.
 *
 * The consumer receives the producer's SQLite bytes through [CloudStore]. It never creates a
 * replacement database from the manifest; the expected values in the manifest are assertions
 * about the state that the worker must reconstruct.
 */
internal suspend fun runCloudArtifactExchange(provider: CloudProvider): CloudArtifactExchangeOutcome {
    val config = requireNotNull(cloudArtifactExchangeConfig()) {
        "Run snapshot exchange with the Gradle cloud artifact tasks or allTests"
    }
    if (config.role == CloudArtifactRole.PRODUCER) {
        return produceCloudArtifact(config, provider, config.artifactPath(provider), config.manifestPath(provider))
    }
    val producers = config.producerPlatforms.filter {
        it != config.platform && (provider != CloudProvider.ICLOUD || it != "android")
    }
    require(producers.isNotEmpty()) { "No supported producer platforms configured for $provider" }
    producers.forEach { producer ->
        val source = config.copy(directory = joinPath(config.directory, producer), runId = producer)
        consumeCloudArtifact(source, provider, source.artifactPath(provider), source.manifestPath(provider))
    }
    return CloudArtifactExchangeOutcome.CONSUMED
}

private suspend fun produceCloudArtifact(
    config: CloudArtifactExchangeConfig,
    provider: CloudProvider,
    artifactPath: String,
    manifestPath: String,
): CloudArtifactExchangeOutcome = coroutineScope {
    val store = CloudStore()
    val device = createCloudDevice(provider, store, "artifact-producer-${config.platform}")
    try {
        device.seed()
        device.read("article-one", true)
        device.bookmark("article-two", true)
        device.backup()
        store.propagate(deviceId = "artifact-producer-${config.platform}")

        val cloudFile = store.snapshot().singleOrNull()
            ?: error("Expected one uploaded cloud database for $provider")
        val bytes = cloudFile.bytes
        require(bytes.startsWithSqliteHeader()) {
            "Cloud artifact is not an SQLite database: $artifactPath"
        }
        assertEquals(CLOUD_ARTIFACT_ACCOUNT, cloudFile.account)
        assertEquals(provider, cloudFile.provider)

        val expectedFlags = device.flags()
        val manifest = CloudArtifactManifest(
            provider = provider,
            producerPlatform = config.platform,
            account = cloudFile.account,
            fileName = cloudFile.name,
            schema = CLOUD_ARTIFACT_SCHEMA,
            byteCount = bytes.size,
            expectedSourceIds = device.database.getFeedSources().map { it.id }.sorted(),
            expectedCategoryIds = device.database.getFeedSourceCategories().map { it.id }.sorted(),
            expectedFlags = expectedFlags,
        )

        // Refuse stale output before writing either half of an artifact pair.
        check(!artifactExists(artifactPath)) { "Cloud artifact already exists: $artifactPath" }
        check(!artifactExists(manifestPath)) { "Cloud artifact manifest already exists: $manifestPath" }
        writeCloudArtifact(artifactPath, bytes)
        writeCloudArtifact(manifestPath, manifest.serialize())
        println(
            "CLOUD_ARTIFACT_PRODUCED provider=$provider platform=${config.platform} " +
                "bytes=${bytes.size} path=$artifactPath",
        )
        CloudArtifactExchangeOutcome.PRODUCED
    } finally {
        device.close()
    }
}

private suspend fun consumeCloudArtifact(
    config: CloudArtifactExchangeConfig,
    provider: CloudProvider,
    artifactPath: String,
    manifestPath: String,
): CloudArtifactExchangeOutcome = coroutineScope {
    check(artifactExists(artifactPath)) { "Missing cloud artifact: $artifactPath" }
    check(artifactExists(manifestPath)) { "Missing cloud artifact manifest: $manifestPath" }

    val manifest = CloudArtifactManifest.parse(readCloudArtifact(manifestPath))
    assertEquals(provider, manifest.provider)
    assertEquals(CLOUD_ARTIFACT_SCHEMA, manifest.schema)
    val bytes = readCloudArtifact(artifactPath)
    assertEquals(manifest.byteCount, bytes.size)
    require(bytes.startsWithSqliteHeader()) { "Cloud artifact is not an SQLite database: $artifactPath" }

    val store = CloudStore()
    val importDeviceId = "artifact-import-${config.platform}"
    store.upload(
        provider = provider,
        account = manifest.account,
        name = manifest.fileName,
        bytes = bytes,
        deviceId = importDeviceId,
    )
    store.propagate(importDeviceId)
    assertContentEquals(bytes, store.download(provider, manifest.account, manifest.fileName))

    val device = createCloudDevice(provider, store, "artifact-consumer-${config.platform}")
    val outcomes = mutableListOf<com.prof18.feedflow.core.model.SyncResult>()
    val collector = launch(start = CoroutineStart.UNDISPATCHED) {
        device.application.koin.get<FeedSyncMessageQueue>().messageQueue.collect { outcomes += it }
    }
    try {
        device.refresh()
        assertEquals(manifest.expectedCategoryIds, device.database.getFeedSourceCategories().map { it.id }.sorted())
        assertEquals(manifest.expectedSourceIds, device.database.getFeedSources().map { it.id }.sorted())
        assertEquals(manifest.expectedFlags, device.flags())
        assertTrue(outcomes.isNotEmpty(), "The consumer worker did not emit a sync completion")
        assertTrue(outcomes.all { !it.isError() }, "Unexpected consumer sync outcomes: $outcomes")
        println(
            "CLOUD_ARTIFACT_CONSUMED provider=$provider producer=${manifest.producerPlatform} " +
                "consumer=${config.platform} bytes=${bytes.size} path=$artifactPath",
        )
        CloudArtifactExchangeOutcome.CONSUMED
    } finally {
        collector.cancel()
        device.close()
    }
}

private fun CloudArtifactExchangeConfig.artifactPath(provider: CloudProvider): String =
    joinPath(directory, "cloud-sync-$runId-${provider.name.lowercase()}.sqlite")

private fun CloudArtifactExchangeConfig.manifestPath(provider: CloudProvider): String =
    joinPath(directory, "cloud-sync-$runId-${provider.name.lowercase()}.manifest")

private fun joinPath(directory: String, fileName: String): String =
    "${directory.trimEnd('/').trimEnd('\\')}/$fileName"

private fun artifactExists(path: String): Boolean = runCatching {
    readCloudArtifact(path)
}.isSuccess

private fun ByteArray.startsWithSqliteHeader(): Boolean =
    contentEquals("SQLite format 3\u0000".encodeToByteArray(), 0, 16, 0, 16)

private const val CLOUD_ARTIFACT_ACCOUNT = "fixture-account"
private const val CLOUD_ARTIFACT_SCHEMA = "FeedFlowFeedSyncDB"

private data class CloudArtifactManifest(
    val provider: CloudProvider,
    val producerPlatform: String,
    val account: String,
    val fileName: String,
    val schema: String,
    val byteCount: Int,
    val expectedSourceIds: List<String>,
    val expectedCategoryIds: List<String>,
    val expectedFlags: Map<String, Pair<Boolean, Boolean>>,
) {
    fun serialize(): ByteArray = buildString {
        appendLine("artifactVersion=1")
        appendLine("provider=${provider.name}")
        appendLine("producerPlatform=$producerPlatform")
        appendLine("account=$account")
        appendLine("fileName=$fileName")
        appendLine("schema=$schema")
        appendLine("byteCount=$byteCount")
        appendLine("expectedSourceIds=${expectedSourceIds.joinToString(",")}")
        appendLine("expectedCategoryIds=${expectedCategoryIds.joinToString(",")}")
        appendLine(
            "expectedFlags=" + expectedFlags.entries.sortedBy { it.key }.joinToString(";") { (id, flags) ->
                "$id,${flags.first},${flags.second}"
            },
        )
    }.encodeToByteArray()

    companion object {
        fun parse(bytes: ByteArray): CloudArtifactManifest {
            val values = bytes.decodeToString().lineSequence()
                .filter { it.isNotEmpty() }
                .associate { line ->
                    val separator = line.indexOf('=')
                    require(separator > 0) { "Malformed cloud artifact manifest line: $line" }
                    line.substring(0, separator) to line.substring(separator + 1)
                }
            require(values["artifactVersion"] == "1") { "Unsupported cloud artifact manifest version" }
            val flags = values.required("expectedFlags").let { encoded ->
                if (encoded.isEmpty()) {
                    emptyMap()
                } else {
                    encoded.split(';').associate { item ->
                        val fields = item.split(',')
                        require(fields.size == 3) { "Malformed expectedFlags entry: $item" }
                        fields[0] to (fields[1].toBooleanStrict() to fields[2].toBooleanStrict())
                    }
                }
            }
            return CloudArtifactManifest(
                provider = CloudProvider.valueOf(values.required("provider")),
                producerPlatform = values.required("producerPlatform"),
                account = values.required("account"),
                fileName = values.required("fileName"),
                schema = values.required("schema"),
                byteCount = values.required("byteCount").toInt(),
                expectedSourceIds = values.required("expectedSourceIds").splitValues(),
                expectedCategoryIds = values.required("expectedCategoryIds").splitValues(),
                expectedFlags = flags,
            )
        }
    }
}

private fun Map<String, String>.required(key: String): String =
    get(key) ?: error("Cloud artifact manifest is missing $key")

private fun String.splitValues(): List<String> = if (isEmpty()) emptyList() else split(',')

private fun ByteArray.contentEquals(
    expected: ByteArray,
    offset: Int,
    end: Int,
    expectedOffset: Int,
    expectedEnd: Int,
): Boolean {
    if (size < end || expected.size < expectedEnd - expectedOffset) return false
    for (index in offset until end) {
        if (this[index] != expected[expectedOffset + index - offset]) return false
    }
    return true
}
