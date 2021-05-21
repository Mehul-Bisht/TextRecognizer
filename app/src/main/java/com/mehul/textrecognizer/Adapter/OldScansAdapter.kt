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
import com.mehul.textrecognizer.scans.ScanMapper
import com.mehul.textrecognizer.scans.State
import kotlinx.android.synthetic.main.old_scan_item.view.*
import java.io.File

class OldScansAdapter(
    val context: Context,
    private val onLongClick: (Int) -> Unit
) : PagingDataAdapter<ScanMapper, OldScansAdapter.OldScansViewHolder>(differCallback) {

    private var onClick: ((ScanMapper, Int) -> Unit)? = null

    fun setOnClick(onClickParam: (ScanMapper, Int) -> Unit) {

        onClick = onClickParam
    }

    inner class OldScansViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.thumbnail
        val checkBox: ImageView = itemView.checked
        val description: TextView = itemView.desc
        val itemBackground: ConstraintLayout = itemView.item_background
    }

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<ScanMapper>() {
            override fun areItemsTheSame(oldItem: ScanMapper, newItem: ScanMapper) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ScanMapper, newItem: ScanMapper) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OldScansViewHolder {
        val viewHolder = OldScansViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.old_scan_item, parent, false)
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

            when (scan.isSelected) {

                State.UNINITIALISED -> {

                    Log.d("$position ","UNINITIALISED")
                    holder.checkBox.visibility = View.GONE
                }

                State.UNSELECTED -> {

                    Log.d("$position ","UNSELECTED")
                    holder.apply {
                        //thumbnail.setPadding(5, 5, 5, 5)
                        checkBox.visibility = View.VISIBLE
                        itemBackground.setBackgroundColor(Color.argb(255, 255, 255, 255))
                        checkBox.setImageDrawable(context.resources.getDrawable(R.drawable.ic_unchecked))
                    }
                }

                State.SELECTED -> {

                    Log.d("$position ","SELECTED")
                    holder.apply {
                        //thumbnail.setPadding(15, 15, 15, 15)
                        checkBox.visibility = View.VISIBLE
                        itemBackground.setBackgroundColor(Color.argb(255, 95, 224, 255))
                        checkBox.setImageDrawable(context.resources.getDrawable(R.drawable.ic_checked))
                    }
                }

                State.UNKNOWN -> {

                    Log.d("$position ","UNKNOWN")
                }
                State.INITIALISED -> {

                    Log.d("$position ","INITIALISED")
                }
            }

            holder.description.text = scan.name
            holder.itemView.setOnClickListener {

                onClick?.invoke(scan, position)
            }

            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(position)
                true
            }

        }
    }

}