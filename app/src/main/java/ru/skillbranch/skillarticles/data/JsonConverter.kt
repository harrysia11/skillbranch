package ru.skillbranch.skillarticles.data

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.*

object JsonConverter {

    // json converter
    val moshi = Moshi.Builder()
        .add(DateAdapter())  // convert Long to Date
        .add(KotlinJsonAdapterFactory())
        .build()


    class DateAdapter {

        @ToJson
        fun toJson(date: Date): Long = date.time

        @FromJson
        fun fromJson(timestamp: Long): Date = Date(timestamp)

    }
}
