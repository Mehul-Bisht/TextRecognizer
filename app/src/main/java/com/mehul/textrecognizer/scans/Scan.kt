package com.mehul.textrecognizer.scans

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "scan")
@Parcelize
data class Scan (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String,
    val recognisedText: String,
    val timeOfStorage: Long,
    val filename: String
): Parcelable {

}