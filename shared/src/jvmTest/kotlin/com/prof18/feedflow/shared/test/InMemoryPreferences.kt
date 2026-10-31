package com.prof18.feedflow.shared.test

import java.util.prefs.AbstractPreferences

// Keeps the JDK's own key/value length checks from AbstractPreferences without touching the user's preferences store.
class InMemoryPreferences(
    parent: InMemoryPreferences? = null,
    name: String = "",
) : AbstractPreferences(parent, name) {
    private val values = mutableMapOf<String, String>()
    private val children = mutableMapOf<String, InMemoryPreferences>()

    override fun putSpi(key: String, value: String) {
        values[key] = value
    }

    override fun getSpi(key: String): String? = values[key]

    override fun removeSpi(key: String) {
        values.remove(key)
    }

    override fun removeNodeSpi() = Unit

    override fun keysSpi(): Array<String> = values.keys.toTypedArray()

    override fun childrenNamesSpi(): Array<String> = children.keys.toTypedArray()

    override fun childSpi(name: String): AbstractPreferences =
        children.getOrPut(name) { InMemoryPreferences(this, name) }

    override fun syncSpi() = Unit

    override fun flushSpi() = Unit
}
