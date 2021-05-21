package com.mehul.textrecognizer.Adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mehul.textrecognizer.R
import com.mehul.textrecognizer.scans.Scan
import com.mehul.textrecognizer.scans.ScanMapper
import com.mehul.textrecognizer.scans.State
import kotlinx.android.synthetic.main.old_scan_item.view.*
import kotlinx.android.synthetic.main.old_scan_item_home.view.*
import java.io.File

class HomeScansAdapter(
    val context: Context
) : PagingDataAdapter<Scan, HomeScansAdapter.OldScansViewHolder>(differCallback) {

    inner class OldScansViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.thumbnail_home
        val description: TextView = itemView.desc_home
    }

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<Scan>() {
            override fun areItemsTheSame(oldItem: Scan, newItem: Scan) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Scan, newItem: Scan) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OldScansViewHolder {
        val viewHolder = OldScansViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.old_scan_item_home, parent, false)
        )

        return viewHolder
    }

    override fun onBindViewHolder(holder: OldScansViewHolder, position: Int) {
        val currentScan = getItem(position)

        currentScan?.let { scan ->

            val file = File(context.getExternalFilesDir(null), scan.filename)
            if (file.exists()) {

                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                Glide.with(context)
                    .load(bitmap)
                    .into(holder.thumbnail)
            }

            holder.description.text = scan.name
        }
    }

}