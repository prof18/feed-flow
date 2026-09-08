package com.prof18.feedflow.core.model

/** A provider positively established that the requested backup does not exist. */
class CloudBackupNotFoundException : RuntimeException("Cloud backup not found")
