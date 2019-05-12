package com.syntax.learn.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.IGNORE
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import com.syntax.learn.model.Bookmark

@Dao
interface BookmarkDao {

  @Query("SELECT * FROM Bookmark ORDER BY name")
  fun loadAll(): LiveData<List<Bookmark>>

  @Query("SELECT * FROM Bookmark WHERE id = :arg0")
  fun loadBookmark(bookmarkId: Long): Bookmark

  @Query("SELECT * FROM Bookmark WHERE id = :arg0")
  fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

  @Insert(onConflict = IGNORE)
  fun insertBookmark(bookmark: Bookmark): Long

  @Update(onConflict = REPLACE)
  fun updateBookmark(bookmark: Bookmark)

  @Delete
  fun deleteBookmark(bookmark: Bookmark)
}