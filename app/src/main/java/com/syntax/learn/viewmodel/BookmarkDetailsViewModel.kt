package com.syntax.learn.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.graphics.Bitmap
import com.syntax.learn.model.Bookmark
import com.syntax.learn.repository.BookmarkRepo
import com.syntax.learn.util.ImageUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class BookmarkDetailsViewModel(application: Application) :
    AndroidViewModel(application) {

  private var bookmarkRepo: BookmarkRepo =
      BookmarkRepo(getApplication())
  private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

  fun getCategories(): List<String> {
    return bookmarkRepo.categories
  }

  fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
    if (bookmarkDetailsView == null) {
      mapBookmarkToBookmarkView(bookmarkId)
    }
    return bookmarkDetailsView
  }

  fun updateBookmark(bookmarkDetailsView: BookmarkDetailsView) {

    launch(CommonPool) {
      val bookmark = bookmarkViewToBookmark(bookmarkDetailsView)
      bookmark?.let { bookmarkRepo.updateBookmark(it) }
    }
  }

  fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
    launch (CommonPool) {
      val bookmark = bookmarkDetailsView.id?.let {
        bookmarkRepo.getBookmark(it)
      }
      bookmark?.let {
        bookmarkRepo.deleteBookmark(it)
      }
    }
  }

  fun getCategoryResourceId(category: String): Int? {
    return bookmarkRepo.getCategoryResourceId(category)
  }

  private fun bookmarkViewToBookmark(bookmarkDetailsView: BookmarkDetailsView):
      Bookmark? {
    val bookmark = bookmarkDetailsView.id?.let {
      bookmarkRepo.getBookmark(it)
    }
    if (bookmark != null) {
      bookmark.id = bookmarkDetailsView.id
      bookmark.name = bookmarkDetailsView.name
      bookmark.phone = bookmarkDetailsView.phone
      bookmark.address = bookmarkDetailsView.address
      bookmark.notes = bookmarkDetailsView.notes
      bookmark.category = bookmarkDetailsView.category
    }
    return bookmark
  }

  private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
    val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
    bookmarkDetailsView = Transformations.map(bookmark) { bookmark ->
      bookmark?.let {
        val bookmarkView = bookmarkToBookmarkView(bookmark)
        bookmarkView
      }
    }
  }

  private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
    return BookmarkDetailsView(
        bookmark.id,
        bookmark.name,
        bookmark.phone,
        bookmark.address,
        bookmark.notes,
        bookmark.category,
        bookmark.longitude,
        bookmark.latitude,
        bookmark.placeId
    )
  }

  data class BookmarkDetailsView(var id: Long? = null,
                                 var name: String = "",
                                 var phone: String = "",
                                 var address: String = "",
                                 var notes: String = "",
                                 var category: String = "",
                                 var longitude: Double = 0.0,
                                 var latitude: Double = 0.0,
                                 var placeId: String? = null) {
    fun getImage(context: Context): Bitmap? {
      id?.let {
        return ImageUtils.loadBitmapFromFile(context,
            Bookmark.generateImageFilename(it))
      }
      return null
    }

    fun setImage(context: Context, image: Bitmap) {
      id?.let {
        ImageUtils.saveBitmapToFile(context, image,
            Bookmark.generateImageFilename(it))
      }
    }
  }

}