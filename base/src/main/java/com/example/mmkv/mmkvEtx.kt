package com.example.mmkv

import com.tencent.mmkv.MMKV


inline fun <reified T> MMKV.get(key: String, def: T): T {
    return when (T::class) {
        String::class -> decodeString(key, def as? String ?: "") as T
        Int::class -> decodeInt(key, def as? Int ?: 0) as T
        Boolean::class -> decodeBool(key, def as? Boolean ?: false) as T
        Long::class -> decodeLong(key, def as? Long ?: 0L) as T
        Float::class -> decodeFloat(key, def as? Float ?: 0f) as T
        Double::class -> decodeDouble(key, def as? Double ?: 0.0) as T
        else -> throw IllegalArgumentException("Unsupported type")
    }
}





inline fun <reified T> MMKV.put(key: String, value: T) {
    when (value) {
        is String -> encode(key, value)
        is Int -> encode(key, value)
        is Boolean -> encode(key, value)
        is Long -> encode(key, value)
        is Float -> encode(key, value)
        is Double -> encode(key, value)
        else -> throw IllegalArgumentException("Unsupported type")
    }
}

