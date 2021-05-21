package com.mehul.textrecognizer.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Mehul Bisht on 07-01-2021
 */

class DateTimeFormatter {

    companion object{

        private const val SECOND_MILLIS = 1000
        private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
        private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS = 24 * HOUR_MILLIS

        fun getTimeAgo(time: Long) : String?{

            val now: Long = System.currentTimeMillis()
            if(time > now || time < 0){
                return null
            }

            val diff = now - time

            return if(diff < MINUTE_MILLIS){
                "just now"
            } else if(diff < 2 * MINUTE_MILLIS){
                "1 min ago"
            } else if(diff < 50 * MINUTE_MILLIS){
                "${(diff / MINUTE_MILLIS)} mins ago"
            } else if(diff < 90 * MINUTE_MILLIS){
                "1 hour ago"
            } else if(diff < 24 * HOUR_MILLIS){
                "${(diff / HOUR_MILLIS)} hours ago"
            } else if(diff < 48 * HOUR_MILLIS){
                "yesterday, at ${getTime(time)}"
            } else {
                //"${(diff / DAY_MILLIS)} days ago"
                getDate(time)
            }
        }

        fun getFormalDate(time: Long) : String{

            val sdfDate = SimpleDateFormat("dd-MM-YYYY")
            val date = Date(time)
            val displayDate = sdfDate.format(date)

            return displayDate
        }

        private fun getDate(time: Long) : String{

            val sdfDate = SimpleDateFormat("MM")

            val month = Date(time)
            val displayMonth = sdfDate.format(month)

            val monthName : String = when(displayMonth){
                "1" -> { "Jan" }
                "2" -> { "Feb" }
                "3" -> { "Mar" }
                "4" -> { "Apr" }
                "5" -> { "May" }
                "6" -> { "Jun" }
                "7" -> { "July" }
                "8" -> { "Aug" }
                "9" -> { "Sept" }
                "10" -> { "Oct" }
                "11" -> { "Nov" }
                "12" -> { "Dec" }
                else -> { "no month" }
            }

            val day = Date(time)
            val displayDate = sdfDate.format(day)

            val formattedDate = "$displayDate $monthName at ${getTime(time)}"

            return formattedDate
        }

        private fun getTime(time: Long) : String{

            val sdfTime = SimpleDateFormat("hh:mm aa")
            val Time = Date(time)
            val displayTime = sdfTime.format(Time)

            return displayTime
        }

    }

}