package com.example.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters


/**
 * 时间格式化工具类
 *
 * 把时间的毫秒值转换为今天 xx:xx 、昨天 xx:xx 、昨天之前的用星期X xx:xx,
 * 但是如果不是这周的就x月x日 xx:xx代替，如果不是今年的那就用x年x月x日 xx:xx代替
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
object TimeUtil {

    // 线程安全的DateTimeFormatter
    private val HOUR_MINUTE_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    private val MONTH_DAY_FORMAT = DateTimeFormatter.ofPattern("M月d日")
    private val FULL_YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日")
    private val WEEK_DAYS =
        arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")

    fun formatTime(millis: Long): String {
        val target = millis.toLocalDateTime()
        val now = LocalDateTime.now()

        return when {
            isToday(target, now) -> "今天 ${formatHourMinute(target)}"
            isYesterday(target, now) -> "昨天 ${formatHourMinute(target)}"
            isSameWeek(target, now) -> "${getWeekDayName(target)} ${formatHourMinute(target)}"
            isSameYear(target, now) -> "${formatMonthDay(target)} ${formatHourMinute(target)}"
            else -> formatFullYear(target)
        }
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    private fun isToday(target: LocalDateTime, now: LocalDateTime): Boolean {
        val todayStart = now.toLocalDate().atStartOfDay()
        return !target.isBefore(todayStart)
    }

    private fun isYesterday(target: LocalDateTime, now: LocalDateTime): Boolean {
        val yesterdayStart = now.toLocalDate().minusDays(1).atStartOfDay()
        val todayStart = now.toLocalDate().atStartOfDay()
        return !target.isBefore(yesterdayStart) && target.isBefore(todayStart)
    }

    private fun isSameWeek(target: LocalDateTime, now: LocalDateTime): Boolean {
        val weekStart = now.toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay()
        return !target.isBefore(weekStart)
    }

    private fun isSameYear(target: LocalDateTime, now: LocalDateTime): Boolean {
        return target.year == now.year
    }

    private fun formatHourMinute(time: LocalDateTime): String {
        return time.format(HOUR_MINUTE_FORMAT)
    }

    private fun formatMonthDay(time: LocalDateTime): String {
        return time.format(MONTH_DAY_FORMAT)
    }

    private fun formatFullYear(time: LocalDateTime): String {
        return time.format(FULL_YEAR_FORMAT)
    }

    private fun getWeekDayName(time: LocalDateTime): String {
        return WEEK_DAYS[time.dayOfWeek.value % 7]
    }

}
