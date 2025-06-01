package com.example.utils.ktx

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


//启动Activity
inline fun <reified T : Activity> Context.startActivityKt(block: Intent.() -> Unit = {}) {
    val intent = Intent(this, T::class.java)
    intent.block()
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}


// 扩展函数：将时间戳转换为可读格式
fun Long.toFormattedDate(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))
}

// 扩展属性：获取当天时间范围
val todayRange: Pair<Long, Long>
    get() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis - 1
        return Pair(start, end)
    }


//dpToPx
fun Context.dp2px(dp: Float): Float = dp * this.resources.displayMetrics.density