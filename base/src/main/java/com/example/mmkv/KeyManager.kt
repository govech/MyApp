package com.example.mmkv


/**
 * 使用方式：
 *          val token = KeyManager.User.TOKEN.value()
 *          KeyManager.User.TOKEN.setValue("hahah")
 */
sealed class KeyManager<T>(val key: String, val default: T) {
    // 用户模块
    object User : KeyManager<String>("user", "") {
        val TOKEN = Key("token", "")      // 嵌套 Key 定义
        val VIP_EXPIRY = Key("vip_expiry", 0L)
    }

    // 应用设置
    object Settings : KeyManager<String>("settings", "") {
        val DARK_MODE = Key("dark_mode", false)
        val LANGUAGE = Key("language", "zh")
    }

    // 内部类用于具体 Key 定义
    class Key<T>(key: String, default: T) : KeyManager<T>(key, default)
}




inline fun <reified T> KeyManager.Key<T>.value(): T = MMKVHelper.get(this)

inline fun <reified T> KeyManager.Key<T>.setValue(value: T) = MMKVHelper.put(this, value)