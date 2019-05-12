package com.syntax.learn.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.syntax.learn.R
import com.syntax.learn.ui.MapsActivity
import com.syntax.learn.viewmodel.MapsViewModel.BookmarkView

class BookmarkListAdapter(
    private var bookmarkData: List<BookmarkView>?,
    private val mapsActivity: MapsActivity) :
    RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

  class ViewHolder(v: View, private val mapsActivity: MapsActivity) : RecyclerView.ViewHolder(v) {
    val nameTextView: TextView =
        v.findViewById(R.id.bookmarkNameTextView) as TextView
    val categoryImageView: ImageView =
        v.findViewById(R.id.bookmarkIcon) as ImageView

    init {
      v.setOnClickListener {
        val bookmarkView = itemView.tag as BookmarkView
        mapsActivity.moveToBookmark(bookmarkView)
      }
    }
  }

  fun setBookmarkData(bookmarks: List<BookmarkView>) {
    this.bookmarkData = bookmarks
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int): BookmarkListAdapter.ViewHolder {
    val vh = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.bookmark_item, parent, false), mapsActivity)
    return vh
  }

  override fun onBindViewHolder(holder: ViewHolder,
                                position: Int) {

    val bookmarkData = bookmarkData ?: return
    val bookmarkViewData = bookmarkData[position]

    holder.itemView.tag = bookmarkViewData
    holder.nameTextView.text = bookmarkViewData.name
    bookmarkViewData.categoryResourceId?.let {
      holder.categoryImageView.setImageResource(it)
    }
  }

  override fun getItemCount(): Int {
    return bookmarkData?.size ?: 0
  }
}