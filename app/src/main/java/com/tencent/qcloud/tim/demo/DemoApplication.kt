package com.tencent.qcloud.tim.demo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.multidex.MultiDex
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import com.tencent.imsdk.v2.*
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.login.LoginForDevActivity
import com.tencent.qcloud.tim.demo.signature.GenerateTestUserSig
import com.tencent.qcloud.tim.demo.thirdpush.OEMPush.HUAWEIHmsMessageService
import com.tencent.qcloud.tim.demo.thirdpush.PushSetting
import com.tencent.qcloud.tim.demo.utils.BrandUtil
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.PrivateConstants
import com.tencent.qcloud.tim.demo.utils.TUIUtils
import com.tencent.qcloud.tuicore.TUIThemeManager
import com.tencent.qcloud.tuicore.util.ErrorMessageConverter
import com.tencent.qcloud.tuicore.util.ToastUtil
import org.json.JSONException
import org.json.JSONObject


class DemoApplication : Application() {
    private val mPushSetting = PushSetting()

    companion object {
        private val TAG = DemoApplication::class.java.simpleName

        @SuppressLint("StaticFieldLeak")
        var mApplication: DemoApplication? = null
            private set
    }

    override fun onCreate() {
        DemoLog.i(TAG, "onCreate")
        super.onCreate()
        mApplication = this
        MultiDex.install(this)
        // bugly上报
        val strategy = UserStrategy(applicationContext)
        strategy.appVersion = V2TIMManager.getInstance().version
        CrashReport.initCrashReport(
            applicationContext,
            PrivateConstants.BUGLY_APPID,
            true,
            strategy
        )

        // 添加 Demo 主题
        TUIThemeManager.addLightTheme(R.style.DemoLightTheme)
        TUIThemeManager.addLivelyTheme(R.style.DemoLivelyTheme)
        TUIThemeManager.addSeriousTheme(R.style.DemoSeriousTheme)

        /**
         * TUIKit的初始化函数
         *
         *  context  应用的上下文，一般为对应应用的ApplicationContext
         *  sdkAppID 您在腾讯云注册应用时分配的sdkAppID
         *  configs  TUIKit的相关配置项，一般使用默认即可，需特殊配置参考API文档
         */
        try {
            val buildInfoJson = JSONObject()
            buildInfoJson.put("buildBrand", BrandUtil.getBuildBrand())
            buildInfoJson.put("buildManufacturer", BrandUtil.getBuildManufacturer())
            buildInfoJson.put("buildModel", BrandUtil.getBuildModel())
            buildInfoJson.put("buildVersionRelease", BrandUtil.getBuildVersionRelease())
            buildInfoJson.put("buildVersionSDKInt", BrandUtil.getBuildVersionSDKInt())
            // 工信部要求 app 在运行期间只能获取一次设备信息。因此 app 获取设备信息设置给 SDK 后，SDK 使用该值并且不再调用系统接口。
            V2TIMManager.getInstance().callExperimentalAPI(
                "setBuildInfo",
                buildInfoJson.toString(),
                object : V2TIMValueCallback<Any?> {
                    override fun onSuccess(o: Any?) {
                        DemoLog.i(TAG, "setBuildInfo success")
                    }

                    override fun onError(code: Int, desc: String) {
                        DemoLog.i(
                            TAG,
                            "setBuildInfo code:$code desc:" + ErrorMessageConverter.convertIMError(
                                code,
                                desc
                            )
                        )
                    }
                })
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        registerActivityLifecycleCallbacks(StatisticActivityLifecycleCallback())
        initLoginStatusListener()
    }

    fun init() {
        TUIUtils.init(this, GenerateTestUserSig.SDKAPPID, null, null)
    }

    fun initPush() {
        mPushSetting.initPush()
    }

    fun bindUserID(userId: String) {
        mPushSetting.bindUserID(userId)
    }

    private fun unBindUserID(userId: String) {
        mPushSetting.unBindUserID(userId)
    }

    private fun unInitPush() {
        mPushSetting.unInitPush()
    }

    private fun initLoginStatusListener() {
        V2TIMManager.getInstance().addIMSDKListener(loginStatusListener)
    }

    private val loginStatusListener: V2TIMSDKListener = object : V2TIMSDKListener() {
        override fun onKickedOffline() {
            ToastUtil.toastLongMessage(getString(R.string.repeat_login_tip))
            logout()
        }

        override fun onUserSigExpired() {
            ToastUtil.toastLongMessage(getString(R.string.expired_login_tip))
            logout()
        }
    }

    fun logout() {
        DemoLog.i(TAG, "logout")
        UserInfo.instance.token = ""
        UserInfo.instance.isAutoLogin = false
        unBindUserID(UserInfo.instance.userId ?: "")
        unInitPush()
        val intent = Intent(this, LoginForDevActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("LOGOUT", true)
        startActivity(intent)
    }

    internal class StatisticActivityLifecycleCallback : ActivityLifecycleCallbacks {
        private var foregroundActivities = 0
        private var isChangingConfiguration = false
        private val unreadListener: V2TIMConversationListener =
            object : V2TIMConversationListener() {
                override fun onTotalUnreadMessageCountChanged(totalUnreadCount: Long) {
                    HUAWEIHmsMessageService.updateBadge(
                        mApplication!!,
                        totalUnreadCount.toInt()
                    )
                }
            }

        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
            DemoLog.i(TAG, "onActivityCreated bundle: $bundle")
            if (bundle != null) { // 若bundle不为空则程序异常结束
                // 重启整个程序
                val intent = Intent(activity, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(intent)
            }
        }

        override fun onActivityStarted(activity: Activity) {
            foregroundActivities++
            if (foregroundActivities == 1 && !isChangingConfiguration) {
                // 应用切到前台
                DemoLog.i(TAG, "application enter foreground")
                V2TIMManager.getOfflinePushManager().doForeground(object : V2TIMCallback {
                    override fun onError(code: Int, desc: String) {
                        DemoLog.e(
                            TAG,
                            "doForeground err = $code, desc = " + ErrorMessageConverter.convertIMError(
                                code,
                                desc
                            )
                        )
                    }

                    override fun onSuccess() {
                        DemoLog.i(TAG, "doForeground success")
                    }
                })
                V2TIMManager.getConversationManager().removeConversationListener(unreadListener)
            }
            isChangingConfiguration = false
        }

        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {
            foregroundActivities--
            if (foregroundActivities == 0) {
                // 应用切到后台
                DemoLog.i(TAG, "application enter background")
                V2TIMManager.getConversationManager()
                    .getTotalUnreadMessageCount(object : V2TIMValueCallback<Long> {
                        override fun onSuccess(aLong: Long) {
                            val totalCount = aLong.toInt()
                            V2TIMManager.getOfflinePushManager()
                                .doBackground(totalCount, object : V2TIMCallback {
                                    override fun onError(code: Int, desc: String) {
                                        DemoLog.e(
                                            TAG,
                                            "doBackground err = $code, desc = " + ErrorMessageConverter.convertIMError(
                                                code,
                                                desc
                                            )
                                        )
                                    }

                                    override fun onSuccess() {
                                        DemoLog.i(TAG, "doBackground success")
                                    }
                                })
                        }

                        override fun onError(code: Int, desc: String) {}
                    })
                V2TIMManager.getConversationManager().addConversationListener(unreadListener)
            }
            isChangingConfiguration = activity.isChangingConfigurations
        }

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

}