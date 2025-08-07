package com.blackfoxis.telros.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: PasswordEntity)

    @Delete
    suspend fun delete(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE folderName = :folderName")
    suspend fun deletePasswordsByFolderName(folderName: String)

    @Query("SELECT * FROM passwords ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE folderName IS NULL OR folderName == '' ORDER BY createdAt DESC")
    fun getGeneratedFlow(): Flow<List<PasswordEntity>>

    @Query("SELECT DISTINCT folderName FROM passwords WHERE folderName IS NOT NULL AND folderName != '' ORDER BY folderName ASC")
    fun getFoldersFlow(): Flow<List<String>>

    @Query("SELECT * FROM passwords WHERE folderName = :folderName ORDER BY createdAt DESC")
    fun getByFolderFlow(folderName: String): Flow<List<PasswordEntity>>

}