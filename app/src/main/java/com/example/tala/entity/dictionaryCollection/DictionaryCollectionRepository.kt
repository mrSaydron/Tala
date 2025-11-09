package com.example.tala.entity.dictionaryCollection

class DictionaryCollectionRepository(
    private val collectionDao: DictionaryCollectionDao,
    private val entryDao: DictionaryCollectionEntryDao
) {

    suspend fun insert(collection: DictionaryCollection): Long = collectionDao.insert(collection)

    suspend fun update(collection: DictionaryCollection): Long = collectionDao.insert(collection)

    suspend fun delete(collection: DictionaryCollection) = collectionDao.delete(collection)

    suspend fun getAll(): List<DictionaryCollection> = collectionDao.getAll()

    suspend fun getById(id: Int): DictionaryCollection? = collectionDao.getById(id)

    suspend fun getByName(name: String): DictionaryCollection? = collectionDao.getByName(name)

    suspend fun getAllWithEntries(): List<DictionaryCollectionWithEntries> = collectionDao.getAllWithEntries()

    suspend fun getByIdWithEntries(id: Int): DictionaryCollectionWithEntries? = collectionDao.getByIdWithEntries(id)

    suspend fun addDictionaryToCollection(collectionId: Int, dictionaryId: Int): Long =
        entryDao.insert(DictionaryCollectionEntry(collectionId, dictionaryId))

    suspend fun addDictionariesToCollection(collectionId: Int, dictionaryIds: List<Int>): List<Long> =
        entryDao.insertAll(dictionaryIds.map { DictionaryCollectionEntry(collectionId, it) })

    suspend fun removeDictionaryFromCollection(collectionId: Int, dictionaryId: Int) =
        entryDao.deleteByCollectionAndDictionary(collectionId, dictionaryId)

    suspend fun clearCollection(collectionId: Int) = entryDao.deleteByCollectionId(collectionId)

    suspend fun getDictionaryIds(collectionId: Int): List<Int> =
        entryDao.getByCollectionId(collectionId).map(DictionaryCollectionEntry::dictionaryId)

    suspend fun replaceCollectionDictionaries(collectionId: Int, dictionaryIds: List<Int>) {
        entryDao.deleteByCollectionId(collectionId)
        if (dictionaryIds.isNotEmpty()) {
            entryDao.insertAll(dictionaryIds.map { DictionaryCollectionEntry(collectionId, it) })
        }
    }
}

