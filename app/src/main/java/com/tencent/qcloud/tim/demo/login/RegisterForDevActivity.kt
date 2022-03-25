package com.tencent.qcloud.tim.demo.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.databinding.RegisterForDevActivityBinding
import com.tencent.qcloud.tim.demo.main.MainActivity
import com.tencent.qcloud.tim.demo.signature.GenerateTestUserSig
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.PermissionUtils
import com.tencent.qcloud.tim.demo.utils.TUIUtils
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.util.ToastUtil

class RegisterForDevActivity : BaseLightActivity() {
    private lateinit var mBinding: RegisterForDevActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivity()
    }

    private fun initActivity() {
        mBinding = RegisterForDevActivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        PermissionUtils.checkPermission(this)
        mBinding.btnRegister.setOnClickListener {
            DemoApplication.mApplication!!.init()
            UserInfo.instance.userId = mBinding.etAccountName.text.toString()
            // 获取userSig函数
            val userSig = GenerateTestUserSig.genTestUserSig(mBinding.etAccountName.text.toString())
            UserInfo.instance.userSig = userSig
            TUIUtils.login(mBinding.etAccountName.text.toString(), userSig, object : V2TIMCallback {
                override fun onError(code: Int, desc: String) {
                    runOnUiThread { ToastUtil.toastLongMessage(getString(R.string.failed_login_tip) + ", errCode = " + code + ", errInfo = " + desc) }
                    DemoLog.i(
                        TAG,
                        "imRegister errorCode = $code, errorInfo = $desc"
                    )
                }

                override fun onSuccess() {
                    UserInfo.instance.isAutoLogin = true
                    val intent = Intent(this@RegisterForDevActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            })
        }
    }

    companion object {
        private val TAG = RegisterForDevActivity::class.java.simpleName
    }

}