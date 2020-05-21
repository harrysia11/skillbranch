package ru.skillbranch.skillarticles.extensions

import java.text.SimpleDateFormat
import java.util.*

const val _SECOND = 1000L
const val _MINUTE = 60 * _SECOND
const val _HOUR = 60 * _MINUTE
const val _DAY = 24 * _HOUR

enum class TimeUnits(val duration: Long){

    SECOND(duration = _SECOND),
    MINUTE(duration = _MINUTE),
    HOUR(duration = _HOUR),
    DAY(duration = _DAY)
}

fun Date.format(pattern: String = "HH:mm:ss dd.MM.yy"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale("ru"))
    return dateFormat.format(this)
}

fun Date.add(value: Int, unit:TimeUnits): Date{
    this.time +=  value * unit.duration
    return this
}

fun Date.humanizeDiff(): String{
    return if((this.time / _DAY).toInt() == (Date().time / _DAY).toInt()){
        this.format("HH:mm")
    }else{
        this.format("dd:MM:yy")
    }
}