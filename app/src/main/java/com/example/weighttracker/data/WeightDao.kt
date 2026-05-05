// app/src/main/java/com/weighttracker/data/WeightDao.kt
package com.weighttracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: WeightEntry)

    @Delete
    suspend fun deleteWeight(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC")
    fun getAllWeights(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getWeightsSince(startTime: Long): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY timestamp ASC")
    fun getAllWeightsAscending(): Flow<List<WeightEntry>>

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM weight_entries")
    fun getCount(): Flow<Int>
}