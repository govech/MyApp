package com.example.data


import com.example.data.model.UserInfo
import com.google.gson.Gson
import com.tencent.mmkv.MMKV

object UserManager {
    private const val KEY_USER_INFO = "user_info"
    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val gson = Gson()

    fun saveUser(userInfo: UserInfo) {
        val json = gson.toJson(userInfo)
        mmkv.encode(KEY_USER_INFO, json)
    }

    fun getUser(): UserInfo? {
        val json = mmkv.decodeString(KEY_USER_INFO)
        return if (json.isNullOrEmpty()) null else gson.fromJson(json, UserInfo::class.java)
    }

    fun getToken(): String? = getUser()?.token

    fun clear() {
        mmkv.remove(KEY_USER_INFO)
    }

    /**
     * 判断用户是否已登录
     * @return 如果存在 Token 则返回 true，表示已登录；否则返回 false
     */
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()
}
