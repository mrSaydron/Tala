package com.example.tala.entity.wordCollection

class WordCollectionRepository(
    private val collectionDao: WordCollectionDao,
    private val entryDao: WordCollectionEntryDao
) {

    suspend fun insert(collection: WordCollection): Long = collectionDao.insert(collection)

    suspend fun update(collection: WordCollection): Long = collectionDao.insert(collection)

    suspend fun delete(collection: WordCollection) = collectionDao.delete(collection)

    suspend fun getAll(): List<WordCollection> = collectionDao.getAll()

    suspend fun getById(id: Int): WordCollection? = collectionDao.getById(id)

    suspend fun getByName(name: String): WordCollection? = collectionDao.getByName(name)

    suspend fun getAllWithEntries(): List<WordCollectionWithEntries> = collectionDao.getAllWithEntries()

    suspend fun getByIdWithEntries(id: Int): WordCollectionWithEntries? = collectionDao.getByIdWithEntries(id)

    suspend fun addWordToCollection(collectionId: Int, wordId: Int): Long =
        entryDao.insert(WordCollectionEntry(collectionId, wordId))

    suspend fun addWordsToCollection(collectionId: Int, wordIds: List<Int>): List<Long> =
        entryDao.insertAll(wordIds.map { WordCollectionEntry(collectionId, it) })

    suspend fun removeWordFromCollection(collectionId: Int, wordId: Int) =
        entryDao.deleteByCollectionAndWord(collectionId, wordId)

    suspend fun clearCollection(collectionId: Int) = entryDao.deleteByCollectionId(collectionId)

    suspend fun getWordIds(collectionId: Int): List<Int> =
        entryDao.getByCollectionId(collectionId).map(WordCollectionEntry::wordId)

    suspend fun replaceCollectionWords(collectionId: Int, wordIds: List<Int>) {
        entryDao.deleteByCollectionId(collectionId)
        if (wordIds.isNotEmpty()) {
            entryDao.insertAll(wordIds.map { WordCollectionEntry(collectionId, it) })
        }
    }
}

