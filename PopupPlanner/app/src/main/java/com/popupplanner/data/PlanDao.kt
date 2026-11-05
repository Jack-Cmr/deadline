package com.popupplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    /** The list screen observes this flow; SQL keeps everything in deadline order. */
    @Query("SELECT * FROM plans ORDER BY triggerAt ASC, id ASC")
    fun observeAll(): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE triggerAt > :now ORDER BY triggerAt ASC, id ASC")
    suspend fun getFuturePlans(now: Long): List<Plan>

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Plan?

    @Insert
    suspend fun insert(plan: Plan): Long

    @Delete
    suspend fun delete(plan: Plan)
}
