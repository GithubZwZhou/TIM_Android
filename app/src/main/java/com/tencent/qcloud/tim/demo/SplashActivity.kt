package com.tencent.qcloud.tim.demo

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.login.LoginForDevActivity
import com.tencent.qcloud.tim.demo.main.MainActivity
import com.tencent.qcloud.tim.demo.thirdpush.OfflineMessageDispatcher
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.TUIUtils
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.util.ToastUtil

class SplashActivity : BaseLightActivity() {
    private lateinit var mUserInfo: UserInfo

    /**
     * 1. 设置背景色
     * 2. 处理登录 [handleData]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        mUserInfo = UserInfo.instance
        handleData()
    }

    /**
     * 根据 SharedPreference 存储的 [mUserInfo] 确定之前是否登录过:
     * + 直接初始化 [DemoApplication.init]，并调用 [login] 登录
     * - 调用 [startLogin] 启动登录
     */
    private fun handleData() {
        if (mUserInfo.isAutoLogin) {
            DemoApplication.mApplication!!.init()
            login()
        } else {
            startLogin()
        }
    }

    /**
     * 调用登录接口 [TUIUtils.login] 进行登录：
     * + 如果成功，则调用 [startMain] 打开 [MainActivity]
     * - 如果失败，调用 [startLogin] 重新登录 [LoginForDevActivity]
     */
    private fun login() {
        if (mUserInfo.userId == null || mUserInfo.userSig == null) {
            ToastUtil.toastLongMessage(getString(R.string.failed_login_tip) + ", " + getString(R.string.empty_string_in_auto_login))
            return
        }
        TUIUtils.login(mUserInfo.userId!!, mUserInfo.userSig!!, object : V2TIMCallback {
            override fun onError(code: Int, desc: String) {
                runOnUiThread {
                    ToastUtil.toastLongMessage(getString(R.string.failed_login_tip) + ", errCode = " + code + ", errInfo = " + desc)
                    startLogin()
                }
                DemoLog.i(TAG, "imLogin errorCode = $code, errorInfo = $desc")
            }

            override fun onSuccess() {
                startMain()
            }
        })
    }

    /**
     * 启动 [LoginForDevActivity]
     */
    private fun startLogin() {
        val intent = Intent(this@SplashActivity, LoginForDevActivity::class.java)
        startActivity(intent)
        finish()
    }


    /**
     * 通过 [getIntent] 获取 离线消息，如果存在则重定向，否则启动 [MainActivity]
     */
    private fun startMain() {
        DemoLog.i(TAG, "MainActivity")
        val bean = OfflineMessageDispatcher.parseOfflineMessage(intent)
        if (bean != null) {
            DemoLog.i(TAG, "startMain offlinePush bean is $bean")
            OfflineMessageDispatcher.redirect(bean)
            DemoApplication.mApplication!!.initPush()
            DemoApplication.mApplication!!.bindUserID(UserInfo.instance.userId ?: "")
            finish()
            return
        }
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        intent.putExtras(getIntent())
        startActivity(intent)
        finish()
    }

    companion object {
        private val TAG = SplashActivity::class.java.simpleName
    }
}