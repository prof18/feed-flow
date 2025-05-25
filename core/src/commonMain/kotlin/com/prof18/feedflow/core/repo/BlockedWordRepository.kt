package com.prof18.feedflow.core.repo

import com.prof18.feedflow.db.FeedFlowDB
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow

interface BlockedWordRepository {
    fun getBlockedWords(): Flow<List<String>>
    suspend fun insertBlockedWord(word: String)
    suspend fun deleteBlockedWord(word: String)
}

internal class BlockedWordRepositoryImpl(
    private val db: FeedFlowDB,
) : BlockedWordRepository {

    override fun getBlockedWords(): Flow<List<String>> =
        db.blockedWordQueries
            .selectAllBlockedWords()
            .asFlow()
            .mapToList()

    override suspend fun insertBlockedWord(word: String) {
        db.blockedWordQueries.insertBlockedWord(word = word)
    }

    override suspend fun deleteBlockedWord(word: String) {
        db.blockedWordQueries.deleteBlockedWord(word = word)
    }
}
