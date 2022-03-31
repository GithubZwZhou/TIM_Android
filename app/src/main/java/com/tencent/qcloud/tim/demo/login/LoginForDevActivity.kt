package com.tencent.qcloud.tim.demo.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.databinding.LoginForDevActivityBinding
import com.tencent.qcloud.tim.demo.main.MainActivity
import com.tencent.qcloud.tim.demo.signature.GenerateTestUserSig
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.PermissionUtils
import com.tencent.qcloud.tim.demo.utils.TUIUtils.login
import com.tencent.qcloud.tuicore.TUIThemeManager
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.util.ToastUtil

/**
 * Demo的登录Activity
 * 用户名可以是任意非空字符，但是前提需要按照下面文档修改代码里的 SDKAPPID 与 PRIVATEKEY
 * https://github.com/tencentyun/TIMSDK/tree/master/Android
 */
class LoginForDevActivity : BaseLightActivity() {
    private lateinit var mBinding: LoginForDevActivityBinding
    private var languageChangedReceiver: BroadcastReceiver? = null
    private var themeChangedReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        languageChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                initActivity()
                setCurrentTheme()
            }
        }
        themeChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                setCurrentTheme()
            }
        }
        val languageFilter = IntentFilter().apply {
            addAction(LanguageSelectActivity.DEMO_LANGUAGE_CHANGED_ACTION)
        }
        val themeFilter = IntentFilter().apply {
            addAction(ThemeSelectActivity.DEMO_THEME_CHANGED_ACTION)
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(languageChangedReceiver!!, languageFilter)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(themeChangedReceiver!!, themeFilter)
        initActivity()
    }

    private fun initActivity() {
        mBinding = LoginForDevActivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        PermissionUtils.checkPermission(this)
        mBinding.loginBtn.setOnClickListener {
            DemoApplication.mApplication!!.init()
            UserInfo.instance.userId = mBinding.loginUser.text.toString()
            // 获取userSig函数
            val userSig = GenerateTestUserSig.genTestUserSig(mBinding.loginUser.text.toString())
            UserInfo.instance.userSig = userSig
            login(mBinding.loginUser.text.toString(), userSig, object : V2TIMCallback {
                override fun onError(code: Int, desc: String) {
                    runOnUiThread { ToastUtil.toastLongMessage(getString(R.string.failed_login_tip) + ", errCode = " + code + ", errInfo = " + desc) }
                    DemoLog.i(TAG, "imLogin errorCode = $code, errorInfo = $desc")
                }

                override fun onSuccess() {
                    UserInfo.instance.isAutoLogin = true
                    val intent = Intent(this@LoginForDevActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            })
        }
        mBinding.loginUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mBinding.loginBtn.isEnabled =
                    !TextUtils.isEmpty(mBinding.loginUser.text) && !TextUtils.isEmpty(mBinding.loginPwd.text)
            }
        })
        mBinding.loginPwd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mBinding.loginBtn.isEnabled =
                    !TextUtils.isEmpty(mBinding.loginUser.text) && !TextUtils.isEmpty(mBinding.loginPwd.text)
            }
        })
        mBinding.loginUser.setText(UserInfo.instance.userId)
        mBinding.loginUser.setText(UserInfo.instance.password)
        mBinding.languageArea.setOnClickListener {
            LanguageSelectActivity.startSelectLanguage(this@LoginForDevActivity)
        }
        mBinding.modifyTheme.setOnClickListener {
            ThemeSelectActivity.startSelectTheme(this@LoginForDevActivity)
        }
        mBinding.goRegisterBtn.setOnClickListener {
            val intent = Intent(this, RegisterForDevActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setCurrentTheme() {
        when (TUIThemeManager.getInstance().currentTheme) {
            TUIThemeManager.THEME_LIGHT -> {
                mBinding.logo.setBackgroundResource(R.drawable.demo_ic_logo_light)
                mBinding.loginBtn.setBackgroundResource(R.drawable.button_border_light)
                mBinding.goRegisterBtn.setBackgroundResource(R.drawable.button_border_light)
            }
            TUIThemeManager.THEME_LIVELY -> {
                mBinding.logo.setBackgroundResource(R.drawable.demo_ic_logo_lively)
                mBinding.loginBtn.setBackgroundResource(R.drawable.button_border_lively)
                mBinding.goRegisterBtn.setBackgroundResource(R.drawable.button_border_lively)
            }
            TUIThemeManager.THEME_SERIOUS -> {
                mBinding.logo.setBackgroundResource(R.drawable.demo_ic_logo_serious)
                mBinding.loginBtn.setBackgroundResource(R.drawable.button_border_serious)
                mBinding.goRegisterBtn.setBackgroundResource(R.drawable.button_border_serious)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 系统请求权限回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PermissionUtils.REQ_PERMISSION_CODE -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ToastUtil.toastLongMessage(getString(R.string.permission_tip))
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (themeChangedReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(themeChangedReceiver!!)
            themeChangedReceiver = null
        }
        if (languageChangedReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(languageChangedReceiver!!)
            languageChangedReceiver = null
        }
    }

    companion object {
        private val TAG = LoginForDevActivity::class.java.simpleName
    }
}