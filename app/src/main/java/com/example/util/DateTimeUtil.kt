package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtil {

    private val manilaTimeZone = TimeZone.getTimeZone("Asia/Manila")

    fun getPhilippineTimeFormatted(epochMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy • hh:mm:ss a 'PST (UTC+8)'", Locale.ENGLISH)
        sdf.timeZone = manilaTimeZone
        return sdf.format(Date(epochMillis))
    }

    fun getPhilippineTimeShort(epochMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("MMM d, hh:mm:ss a", Locale.ENGLISH)
        sdf.timeZone = manilaTimeZone
        return sdf.format(Date(epochMillis))
    }

    fun getPhilippineTimeIso(epochMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMillis))
    }
}
