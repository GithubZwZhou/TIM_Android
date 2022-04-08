package com.tencent.qcloud.tim.demo.bean

import com.google.gson.Gson
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.utils.TUIKitConstants
import java.io.Serializable
import kotlin.jvm.Synchronized

class UserInfo private constructor() : Serializable {
    var zone: String? = null
        set(zone) {
            field = zone
            setUserInfo(this)
        }
    var phone: String? = null
        set(userPhone) {
            field = userPhone
            setUserInfo(this)
        }
    var token: String? = null
        set(token) {
            field = token
            setUserInfo(this)
        }
    var userId: String? = null
        set(userId) {
            field = userId
            setUserInfo(this)
        }
    var userSig: String? = null
        set(userSig) {
            field = userSig
            setUserInfo(this)
        }
    var name: String? = null
        set(name) {
            field = name
            setUserInfo(this)
        }
    var password: String? = null
        set(password) {
            field = password
            setUserInfo(this)
        }
    var avatar: String? = null
        set(url) {
            field = url
            setUserInfo(this)
        }
    var isAutoLogin = false
        set(autoLogin) {
            field = autoLogin
            setUserInfo(this)
        }
    var userActualName: String? = ""
        set(value) {
            field = value
            setUserInfo(this)
        }
    var emergencyName: String? = ""
        set(value) {
            field = value
            setUserInfo(this)
        }
    var emergencyNumber: String? = ""
        set(value) {
            field = value
            setUserInfo(this)
        }
    var userIdLocal: String? = ""
        set(value) {
            field = value
            setUserInfo(this)
        }

    private fun setUserInfo(info: UserInfo) {
        val shareInfo =
            DemoApplication.mApplication!!.getSharedPreferences(TUIKitConstants.USERINFO, 0)
        val editor = shareInfo.edit()
        editor.putString(PER_USER_MODEL, Gson().toJson(info))
        editor.apply()
    }

    companion object {
        private const val PER_USER_MODEL = "per_user_model"
        private var sUserInfo: UserInfo? = null

        @JvmStatic
        @get:Synchronized
        val instance: UserInfo
            get() {
                if (sUserInfo == null) {
                    val shareInfo = DemoApplication.mApplication!!.getSharedPreferences(
                        TUIKitConstants.USERINFO,
                        0
                    )
                    val json = shareInfo.getString(PER_USER_MODEL, "")
                    sUserInfo = Gson().fromJson(json, UserInfo::class.java)
                    if (sUserInfo == null) {
                        sUserInfo = UserInfo()
                    }
                }
                return sUserInfo!!
            }
    }
}