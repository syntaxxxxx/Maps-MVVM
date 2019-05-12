package com.syntax.learn.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.syntax.learn.R
import com.syntax.learn.util.ImageUtils
import com.syntax.learn.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.*
import java.io.File
import java.net.URLEncoder

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

  private lateinit var bookmarkDetailsViewModel:
      BookmarkDetailsViewModel
  private var bookmarkDetailsView:
      BookmarkDetailsViewModel.BookmarkDetailsView? = null
  private var photoFile: File? = null


  override fun onCaptureClick() {

    photoFile = null
    try {

      photoFile = ImageUtils.createUniqueImageFile(this)

      if (photoFile == null) {
        return
      }
    } catch (ex: java.io.IOException) {
      return
    }

    val captureIntent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    val photoUri = FileProvider.getUriForFile(this,
        "com.syntax.learn.fileprovider",
        photoFile)

    captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
        photoUri)

    val intentActivities = packageManager.queryIntentActivities(
        captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
    intentActivities.map { it.activityInfo.packageName }
        .forEach { grantUriPermission(it, photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }

    startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
  }

  override fun onPickClick() {
    val pickIntent = Intent(Intent.ACTION_PICK,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
  }

  override fun onCreate(savedInstanceState:
                        android.os.Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bookmark_details)
    setupToolbar()
    setupViewModel()
    getIntentData()
    setupFab()
  }

  override fun onCreateOptionsMenu(menu: android.view.Menu):
      Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_bookmark_details, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_save -> {
        saveChanges()
        return true
      }
      R.id.action_delete -> {
        deleteBookmark()
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int,
                                data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == android.app.Activity.RESULT_OK) {

      when (requestCode) {

        REQUEST_CAPTURE_IMAGE -> {

          val photoFile = photoFile ?: return

          val uri = FileProvider.getUriForFile(this,
              "com.syntax.learn.fileprovider",
              photoFile)
          revokeUriPermission(uri,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

          val image = getImageWithPath(photoFile.absolutePath)
          image?.let { updateImage(it) }
        }

        REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
          val imageUri = data.data
          val image = getImageWithAuthority(imageUri)
          image?.let { updateImage(it) }
        }
      }
    }
  }

  private fun populateCategoryList() {

    val bookmarkView = bookmarkDetailsView ?: return

    val resourceId =
        bookmarkDetailsViewModel.getCategoryResourceId(
            bookmarkView.category)

    resourceId?.let { imageViewCategory.setImageResource(it) }

    val categories = bookmarkDetailsViewModel.getCategories()

    val adapter = ArrayAdapter(this,
        android.R.layout.simple_spinner_item, categories)
    adapter.setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item)

    spinnerCategory.adapter = adapter

    val placeCategory = bookmarkView.category
    spinnerCategory.setSelection(
        adapter.getPosition(placeCategory))

    spinnerCategory.post {
      spinnerCategory.onItemSelectedListener = object :
          AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View,
                                    position: Int, id: Long) {

          val category = parent.getItemAtPosition(position) as String
          val resourceId =
              bookmarkDetailsViewModel.getCategoryResourceId(category)
          resourceId?.let {
            imageViewCategory.setImageResource(it) }
        }
        override fun onNothingSelected(parent: AdapterView<*>) {
          // NOTE: This method is required but not used.
        }
      }
    }
  }

  private fun getImageWithAuthority(uri: Uri): Bitmap? {
    return ImageUtils.decodeUriStreamToSize(uri,
        resources.getDimensionPixelSize(
            R.dimen.default_image_width),
        resources.getDimensionPixelSize(
            R.dimen.default_image_height),
        this)
  }

  private fun updateImage(image: Bitmap) {
    val bookmarkView = bookmarkDetailsView ?: return
    imageViewPlace.setImageBitmap(image)
    bookmarkView.setImage(this, image)
  }

  private fun getImageWithPath(filePath: String): Bitmap? {
    return ImageUtils.decodeFileToSize(filePath,
        resources.getDimensionPixelSize(
            R.dimen.default_image_width),
        resources.getDimensionPixelSize(
            R.dimen.default_image_height))
  }

  private fun replaceImage() {
    val newFragment = PhotoOptionDialogFragment.newInstance(this)
    newFragment?.show(supportFragmentManager, "photoOptionDialog")
  }

  private fun saveChanges() {
    val name = editTextName.text.toString()
    if (name.isEmpty()) {
      return
    }
    bookmarkDetailsView?.let { bookmarkView ->
      bookmarkView.name = editTextName.text.toString()
      bookmarkView.notes = editTextNotes.text.toString()
      bookmarkView.address = editTextAddress.text.toString()
      bookmarkView.phone = editTextPhone.text.toString()
      bookmarkView.category = spinnerCategory.selectedItem as String
      bookmarkDetailsViewModel.updateBookmark(bookmarkView)
    }
    finish()
  }

  private fun deleteBookmark()
  {
    val bookmarkView = bookmarkDetailsView ?: return
    AlertDialog.Builder(this)
        .setMessage("Delete?")
        .setPositiveButton("Ok") { _, _ ->
          bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
          finish() }
        .setNegativeButton("Cancel", null)
        .create().show()
  }

  private fun getIntentData() {

    val bookmarkId = intent.getLongExtra(
        MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)

    bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
        this, Observer<BookmarkDetailsViewModel.BookmarkDetailsView> {

      it?.let {
        bookmarkDetailsView = it
        // Populate fields from bookmark
        populateFields()
        populateImageView()
        populateCategoryList()
      }
    })
  }

  private fun setupViewModel() {
    bookmarkDetailsViewModel =
        ViewModelProviders.of(this).get(
            BookmarkDetailsViewModel::class.java)
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
  }

  private fun setupFab() {
    fab.setOnClickListener { sharePlace() }
  }

  private fun populateFields() {
    bookmarkDetailsView?.let { bookmarkView ->
      editTextName.setText(bookmarkView.name)
      editTextPhone.setText(bookmarkView.phone)
      editTextNotes.setText(bookmarkView.notes)
      editTextAddress.setText(bookmarkView.address)
    }
  }

  private fun populateImageView() {
    bookmarkDetailsView?.let { bookmarkView ->
      val placeImage = bookmarkView.getImage(this)
      placeImage?.let {
        imageViewPlace.setImageBitmap(placeImage)
      }
    }
    imageViewPlace.setOnClickListener {
      replaceImage()
    }
  }

  private fun sharePlace() {

    val bookmarkView = bookmarkDetailsView ?: return

    var mapUrl = ""
    if (bookmarkView.placeId == null) {

      val location = URLEncoder.encode("${bookmarkView.latitude},"
          + "${bookmarkView.longitude}", "utf-8")
      mapUrl = "https://www.google.com/maps/dir/?api=1" +
          "&destination=$location"
    } else { // 4
      val name = URLEncoder.encode(bookmarkView.name, "utf-8")
      mapUrl = "https://www.google.com/maps/dir/?api=1" +
          "&destination=$name&destination_place_id=" +
          "${bookmarkView.placeId}"
    }

    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND

    sendIntent.putExtra(Intent.EXTRA_TEXT,
        "Check out ${bookmarkView.name} at:\n$mapUrl")
    sendIntent.putExtra(Intent.EXTRA_SUBJECT,
        "Sharing ${bookmarkView.name}")

    sendIntent.type = "text/plain"

    startActivity(sendIntent)
  }

  companion object {
    private const val REQUEST_CAPTURE_IMAGE = 1
    private const val REQUEST_GALLERY_IMAGE = 2
  }
}