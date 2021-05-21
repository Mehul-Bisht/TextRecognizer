package com.mehul.textrecognizer.scans

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert
    fun insertScan(scan: Scan)

    @Insert
    fun insertMultipleScans(vararg scans: Scan)

    @Delete
    fun deleteScan(scan: Scan)

    @Query("SELECT id FROM scan")
    fun getAllIds(): Flow<List<Int>>

    @Query("DELETE FROM scan WHERE id IN (:ids)")
    fun deleteMultipleScans(ids: List<Int>)

    @Query("SELECT * FROM scan ORDER BY timeOfStorage DESC")
    fun getAllScans(): PagingSource<Int,Scan>

    @Query("SELECT * FROM scan ORDER BY timeOfStorage DESC")
    fun getAllScansList(): Flow<List<Scan>>

    @Query("SELECT * FROM scan ORDER BY timeOfStorage DESC LIMIT 6 OFFSET :offset")
    fun getTenRecentScans(offset: Int): List<Scan>

    @Query("SELECT COUNT(timeOfStorage) FROM scan")
    fun getTotalItems(): Int
}