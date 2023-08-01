package com.prof18.feedflow.utils

internal fun <K, V> Map<K, V>.getValueOrNull(key: K): V? {
    if (!containsKey(key)) {
        return null
    }
    return get(key)
}