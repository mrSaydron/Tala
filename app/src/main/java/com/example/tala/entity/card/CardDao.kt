package com.example.tala.entity.card

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: Card)

    @Insert
    suspend fun insertAll(cards: List<Card>)

    @Update
    suspend fun update(card: Card)

    @Update
    suspend fun updateAll(cards: List<Card>)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM card")
    fun getAll(): LiveData<List<Card>>

    @Query("SELECT * FROM card WHERE cardType = :cardType AND commonId = :commonId")
    suspend fun byTypeAndCommonId(cardType: CardTypeEnum, commonId: String?): Card?

    @Query("SELECT * FROM card WHERE commonId = :commonId")
    suspend fun getByCommonId(commonId: String): List<Card>

    @Query("SELECT * FROM card WHERE cardType = :cardType")
    fun getAllByType(cardType: CardTypeEnum): LiveData<List<Card>>

    @Query("""
        SELECT * 
        FROM card
        WHERE nextReviewDate < :endFindDate 
        ORDER BY nextReviewDate ASC 
        LIMIT 1""")
    suspend fun getNextToReview(endFindDate: Long): Card?

    @Query("""
        SELECT * 
        FROM card 
        WHERE collectionId = :collectionId AND nextReviewDate < :endFindDate 
        ORDER BY nextReviewDate ASC 
        LIMIT 1""")
    suspend fun getNextToReview(collectionId: Int, endFindDate: Long): Card?

    @Query("UPDATE card SET nextReviewDate = :nextReviewDate, intervalMinutes = :intervalMinutes WHERE id = :id")
    suspend fun update(id: Int, nextReviewDate: Long, intervalMinutes: Int)

    @Query("SELECT COUNT(*) FROM card WHERE nextReviewDate < :endFindDate AND status = :status")
    fun getCountToReview(endFindDate: Long, status: StatusEnum): LiveData<Int>

    @Query("""
        SELECT COUNT(*) 
        FROM card 
        WHERE nextReviewDate < :endFindDate 
        AND status = :status 
        AND collectionId = :collectionId""")
    fun getCountToReview(
        endFindDate: Long,
        status: StatusEnum,
        collectionId: Int,
    ): LiveData<Int>

    @Query("SELECT * FROM card WHERE collectionId = :collectionId")
    fun getByCollection(collectionId: Int): LiveData<List<Card>>

    @Query("SELECT * FROM card WHERE collectionId = :collectionId AND cardType = :cardType")
    fun getAllByTypeAndCollection(cardType: CardTypeEnum, collectionId: Int): LiveData<List<Card>>

    @Query("SELECT COUNT(*) FROM card WHERE collectionId = :collectionId")
    fun getCountByCollection(collectionId: Int): LiveData<Int>

    @Query("DELETE FROM card")
    suspend fun deleteAll()

    @Query("DELETE FROM card WHERE commonId = :commonId")
    suspend fun delete(commonId: String)

    @Query("DELETE FROM card WHERE collectionId = :collectionId")
    suspend fun deleteByCollection(collectionId: Int)

}