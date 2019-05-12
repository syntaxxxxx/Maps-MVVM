package com.syntax.learn.ui

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.PlacePhotoMetadata
import com.google.android.gms.location.places.Places
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.syntax.learn.R
import com.syntax.learn.adapter.BookmarkInfoWindowAdapter
import com.syntax.learn.adapter.BookmarkListAdapter
import com.syntax.learn.viewmodel.MapsViewModel
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.android.synthetic.main.main_view_maps.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.OnConnectionFailedListener {

  private lateinit var map: GoogleMap
  private lateinit var fusedLocationClient: FusedLocationProviderClient
  private lateinit var googleApiClient: GoogleApiClient
  private lateinit var mapsViewModel: MapsViewModel
  private lateinit var bookmarkListAdapter: BookmarkListAdapter
  private var markers = HashMap<Long, Marker>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    setupLocationClient()
    setupToolbar()
    setupGoogleClient()
    setupNavigationDrawer()
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap

    setupMapListeners()
    setupViewModel()
    getCurrentLocation()
  }

  override fun onConnectionFailed(connectionResult: ConnectionResult) {
    Log.e(TAG, "Google play connection failed: " + connectionResult.errorMessage)
  }

  override fun onRequestPermissionsResult(requestCode: Int,
                                          permissions: Array<String>,
                                          grantResults: IntArray) {
    if (requestCode == REQUEST_LOCATION) {
      if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        getCurrentLocation()
      } else {
        Log.e(TAG, "Location permission denied")
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int,
                                data: Intent?) {

    when (requestCode) {
      AUTOCOMPLETE_REQUEST_CODE ->
        if (resultCode == Activity.RESULT_OK && data != null) {
          val place = PlaceAutocomplete.getPlace(this, data)
          val location = Location("")
          location.latitude =  place.latLng.latitude
          location.longitude = place.latLng.longitude
          updateMapToLocation(location)
          showProgress()
          displayPoiGetPhotoMetaDataStep(place)
        }
    }
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
    val toggle = ActionBarDrawerToggle(
        this,  drawerLayout, toolbar,
        R.string.open_drawer, R.string.close_drawer)
    toggle.syncState()
  }

  private fun setupMapListeners() {
    map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
    map.setOnPoiClickListener {
      displayPoi(it)
    }
    map.setOnInfoWindowClickListener {
      handleInfoWindowClick(it)
    }
    fab.setOnClickListener {
      searchAtCurrentLocation()
    }
    map.setOnMapLongClickListener { latLng ->
      newBookmark(latLng)
    }
  }

  private fun setupViewModel() {
    mapsViewModel =
        ViewModelProviders.of(this).get(MapsViewModel::class.java)
    createBookmarkObserver()
  }
  
  private fun setupGoogleClient() {
    googleApiClient = GoogleApiClient.Builder(this)
        .enableAutoManage(this, this)
        .addApi(Places.GEO_DATA_API)
        .build()
  }
  
  private fun setupLocationClient() {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
  }

  private fun setupNavigationDrawer() {
    val layoutManager = LinearLayoutManager(this)
    bookmarkRecyclerView.layoutManager = layoutManager
    bookmarkListAdapter = BookmarkListAdapter(null, this)
    bookmarkRecyclerView.adapter = bookmarkListAdapter
  }

  private fun disableUserInteraction() {
    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
  }
  private fun enableUserInteraction() {
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
  }

  private fun newBookmark(latLng: LatLng) {
    launch(CommonPool) {
      val bookmarkId = mapsViewModel.addBookmark(latLng)
      bookmarkId?.let {
        startBookmarkDetails(it)
      }
    }
  }

  private fun showProgress() {
    progressBar.visibility = ProgressBar.VISIBLE
    disableUserInteraction()
  }

  private fun hideProgress() {
    progressBar.visibility = ProgressBar.GONE
    enableUserInteraction()
  }

  private fun updateMapToLocation(location: Location) {
    val latLng = LatLng(location.latitude, location.longitude)
    map.animateCamera(
        CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
  }

  fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {

    drawerLayout.closeDrawer(drawerView)

    val marker = markers[bookmark.id]

    marker?.showInfoWindow()

    val location = Location("")
    location.latitude =  bookmark.location.latitude
    location.longitude = bookmark.location.longitude
    updateMapToLocation(location)
  }

  private fun startBookmarkDetails(bookmarkId: Long) {
    val intent = Intent(this, BookmarkDetailsActivity::class.java)
    intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
    startActivity(intent)
  }

  private fun handleInfoWindowClick(marker: Marker) {
    when (marker.tag) {
      is MapsActivity.PlaceInfo -> {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null && placeInfo.image != null) {
          launch(CommonPool) {
            mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                placeInfo.image)
          }
        }
        marker.remove()
      }
      is MapsViewModel.BookmarkView -> {
        val bookmarkMarkerView = (marker.tag as
            MapsViewModel.BookmarkView)
        marker.hideInfoWindow()
        bookmarkMarkerView.id?.let {
          startBookmarkDetails(it)
        }
      }
    }
  }

  private fun createBookmarkObserver() {
    mapsViewModel.getBookmarkViews()?.observe(
        this, android.arch.lifecycle
        .Observer<List<MapsViewModel.BookmarkView>> {

          map.clear()
          markers.clear()

          it?.let {
            displayAllBookmarks(it)
            bookmarkListAdapter.setBookmarkData(it)
          }
        })
  }
  
  private fun displayAllBookmarks(
      bookmarks: List<MapsViewModel.BookmarkView>) {
    for (bookmark in bookmarks) {
      addPlaceMarker(bookmark)
    }
  }
  
  private fun addPlaceMarker(
      bookmark: MapsViewModel.BookmarkView): Marker? {
    val marker = map.addMarker(MarkerOptions()
        .position(bookmark.location)
        .title(bookmark.name)
        .snippet(bookmark.phone)
        .icon(bookmark.categoryResourceId?.let {
          BitmapDescriptorFactory.fromResource(it)
        })
        .alpha(0.8f))
    marker.tag = bookmark
    bookmark.id?.let { markers.put(it, marker) }
    return marker
  }

  private fun displayPoi(pointOfInterest: PointOfInterest) {
    showProgress()
    displayPoiGetPlaceStep(pointOfInterest)
  }

  private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
    Places.GeoDataApi.getPlaceById(googleApiClient,
        pointOfInterest.placeId)

        .setResultCallback { places ->

          if (places.status.isSuccess && places.count > 0) {
            val place = places.get(0).freeze()
            displayPoiGetPhotoMetaDataStep(place)
          } else {
            Log.e(TAG,
                "Error with getPlaceById ${places.status.statusMessage}")
            hideProgress()
          }
          places.release()
        }
  }

  private fun displayPoiGetPhotoMetaDataStep(place: Place) {
    Places.GeoDataApi.getPlacePhotos(googleApiClient, place.id)
        .setResultCallback { placePhotoMetadataResult ->

          if (placePhotoMetadataResult.status.isSuccess) {

            val photoMetadataBuffer = placePhotoMetadataResult.photoMetadata

            if (photoMetadataBuffer.count > 0) {
              val photo = photoMetadataBuffer.get(0).freeze()
              displayPoiGetPhotoStep(place, photo)
            } else {
              hideProgress()
            }
            photoMetadataBuffer.release()
          } else {
            hideProgress()
          }
        }
  }

  private fun displayPoiGetPhotoStep(place: Place, photo: PlacePhotoMetadata) {
    photo.getScaledPhoto(googleApiClient,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height))
        .setResultCallback { placePhotoResult ->

      hideProgress()
      if (placePhotoResult.status.isSuccess) {
        val image = placePhotoResult.bitmap
        displayPoiDisplayStep(place, image)
      } else {
        displayPoiDisplayStep(place, null)
      }
    }
  }

  private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
    val marker = map.addMarker(MarkerOptions()
        .position(place.latLng)
        .title(place.name as String?)
        .snippet(place.phoneNumber as String?)

    )
    marker?.tag = PlaceInfo(place, photo)
    marker?.showInfoWindow()
  }

  private fun getCurrentLocation() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      requestLocationPermissions()
    } else {
      map.isMyLocationEnabled = true

      fusedLocationClient.lastLocation.addOnCompleteListener {
        if (it.result != null) {
          val latLng = LatLng(it.result.latitude, it.result.longitude)
          val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
          map.moveCamera(update)
        } else {
          Log.e(TAG, "No location found")
        }
      }
    }
  }

  private fun requestLocationPermissions() {
    ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        REQUEST_LOCATION)
  }

  private fun searchAtCurrentLocation() {

    val bounds = map.projection.visibleRegion.latLngBounds
    try {
      val intent = PlaceAutocomplete.IntentBuilder(
          PlaceAutocomplete.MODE_OVERLAY)
          .setBoundsBias(bounds)
          .build(this)
      startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    } catch (e: GooglePlayServicesRepairableException) {
      //TODO: Handle exception
    } catch (e: GooglePlayServicesNotAvailableException) {
      //TODO: Handle exception
    }
  }

  companion object {
    const val EXTRA_BOOKMARK_ID = "com.syntax.learn.EXTRA_BOOKMARK_ID"
    private const val REQUEST_LOCATION = 1
    private const val TAG = "MapsActivity"
    private const val AUTOCOMPLETE_REQUEST_CODE = 2
  }
  class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)
}