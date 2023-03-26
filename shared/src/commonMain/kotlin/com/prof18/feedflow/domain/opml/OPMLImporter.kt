package com.prof18.feedflow.domain.opml

expect class OPMLImporter {
    suspend fun getOPML(opmlInput: OPMLInput): String
}