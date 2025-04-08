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
        WHERE categoryId = :categoryId AND nextReviewDate < :endFindDate 
        ORDER BY nextReviewDate ASC 
        LIMIT 1""")
    suspend fun getNextToReview(categoryId: Int, endFindDate: Long): Card?

    @Query("UPDATE card SET nextReviewDate = :nextReviewDate, interval = :interval WHERE id = :id")
    suspend fun update(id: Int, nextReviewDate: Long, interval: Int)

    @Query("SELECT COUNT(*) FROM card WHERE nextReviewDate < :endFindDate AND status = :status")
    fun getCountToReview(endFindDate: Long, status: StatusEnum): LiveData<Int>

    @Query("UPDATE card SET imagePath = :imagePath WHERE id = :id")
    suspend fun updateImagePath(id: Int, imagePath: String)

    @Query("SELECT * FROM card WHERE categoryId = :categoryId")
    fun getByCategory(categoryId: Int): LiveData<List<Card>>

    @Query("SELECT * FROM card WHERE categoryId = :categoryId AND cardType = :cardType")
    fun getAllByTypeAndCategory(cardType: CardTypeEnum, categoryId: Int): LiveData<List<Card>>

    @Query("DELETE FROM card")
    suspend fun deleteAll()

    @Query("DELETE FROM card WHERE commonId = :commonId")
    fun delete(commonId: String)

}