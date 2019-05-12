package com.syntax.learn.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.model.LatLng
import com.syntax.learn.model.Bookmark
import com.syntax.learn.repository.BookmarkRepo
import com.syntax.learn.util.ImageUtils

class MapsViewModel(application: Application) : AndroidViewModel(application) {

  private val TAG = "MapsViewModel"

  private var bookmarkRepo: BookmarkRepo = BookmarkRepo(
      getApplication())
  private var bookmarks: LiveData<List<BookmarkView>>?
      = null

  fun addBookmark(latLng: LatLng) : Long? {
    val bookmark = bookmarkRepo.createBookmark()
    bookmark.name = "Untitled"
    bookmark.longitude = latLng.longitude
    bookmark.latitude = latLng.latitude
    bookmark.category = "Other"
    return bookmarkRepo.addBookmark(bookmark)
  }

  fun addBookmarkFromPlace(place: Place, image: Bitmap) {

    val bookmark = bookmarkRepo.createBookmark()
    bookmark.placeId = place.id
    bookmark.name = place.name.toString()
    bookmark.longitude = place.latLng.longitude
    bookmark.latitude = place.latLng.latitude
    bookmark.phone = place.phoneNumber.toString()
    bookmark.address = place.address.toString()
    bookmark.category = getPlaceCategory(place)

    val newId = bookmarkRepo.addBookmark(bookmark)
    bookmark.setImage(image, getApplication())
    Log.i(TAG, "New bookmark $newId added to the database.")
  }

  fun getBookmarkViews() :
      LiveData<List<BookmarkView>>? {
    if (bookmarks == null) {
      mapBookmarksToBookmarkView()
    }
    return bookmarks
  }

  private fun mapBookmarksToBookmarkView() {

    val allBookmarks = bookmarkRepo.allBookmarks

    bookmarks = Transformations.map(allBookmarks) { bookmarks ->
      val bookmarkMarkerViews = bookmarks.map { bookmark ->
        bookmarkToBookmarkView(bookmark)
      }
      bookmarkMarkerViews
    }
  }

  private fun getPlaceCategory(place: Place): String {

    var category = "Other"
    val placeTypes = place.placeTypes

    if (placeTypes.size > 0) {
      val placeType = placeTypes[0]
      category = bookmarkRepo.placeTypeToCategory(placeType)
    }
    return category
  }

  private fun bookmarkToBookmarkView(bookmark: Bookmark):
      MapsViewModel.BookmarkView {
    return MapsViewModel.BookmarkView(
        bookmark.id,
        LatLng(bookmark.latitude, bookmark.longitude),
        bookmark.name,
        bookmark.phone,
        bookmarkRepo.getCategoryResourceId(bookmark.category))
  }

  data class BookmarkView(val id: Long? = null,
                          val location: LatLng = LatLng(0.0, 0.0),
                          val name: String = "",
                          val phone: String = "",
                          val categoryResourceId: Int? = null) {
    fun getImage(context: Context): Bitmap? {
      id?.let {
        return ImageUtils.loadBitmapFromFile(context,
            Bookmark.generateImageFilename(it))
      }
      return null
    }
  }
}