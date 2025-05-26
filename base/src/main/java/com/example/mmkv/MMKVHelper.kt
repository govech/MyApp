package com.example.mmkv

import com.tencent.mmkv.MMKV

// MMKVHelper.kt
object MMKVHelper {
    // 默认实例（全局）
    private val defaultKV = MMKV.defaultMMKV()

    // 按模块隔离的实例
    private val userKV = MMKV.mmkvWithID("user_data")
    private val settingsKV = MMKV.mmkvWithID("app_settings")

    // 获取对应实例
    fun getKV(keyManager: KeyManager<*>): MMKV = when (keyManager) {
        is KeyManager.User -> userKV
        is KeyManager.Settings -> settingsKV
        else -> defaultKV
    }

    // 统一存取方法
    inline fun <reified T> get(key: KeyManager.Key<T>): T {
        val kv = getKV(key)
        return kv.get(key.key, key.default)
    }

    inline fun <reified T> put(key: KeyManager.Key<T>, value: T) {
        val kv = getKV(key)
        kv.put(key.key, value)
    }
}