package com.tencent.qcloud.tim.demo.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.databinding.RegisterForDevActivityBinding
import com.tencent.qcloud.tim.demo.main.MainActivity
import com.tencent.qcloud.tim.demo.signature.GenerateTestUserSig
import com.tencent.qcloud.tim.demo.utils.ClickPresenter
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.PermissionUtils
import com.tencent.qcloud.tim.demo.utils.TUIUtils
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.util.ToastUtil

class RegisterForDevActivity : BaseLightActivity(), ClickPresenter {
    private lateinit var mBinding: RegisterForDevActivityBinding
    private val mViewModel: RegisterViewModel by viewModels {
        RegisterViewModelProvider()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivity()
    }

    private fun initActivity() {
        mBinding = RegisterForDevActivityBinding.inflate(layoutInflater)
        mBinding.presenter = this
        setContentView(mBinding.root)
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        PermissionUtils.checkPermission(this)

        mViewModel.message.observe(this) {
            Log.e(TAG, it)
            runOnUiThread { ToastUtil.toastShortMessage(it) }
        }
    }


    companion object {
        private val TAG = RegisterForDevActivity::class.java.simpleName
    }

    override fun onClick(v: View) {
        when (v.id) {
            mBinding.btnRegister.id -> {
                DemoApplication.mApplication!!.init()
                UserInfo.instance.apply {
                    userActualName = mBinding.etActualName.text?.toString()
                    phone = mBinding.etPhoneNum.text?.toString()
                    userId = phone // 使用手机号作为 id
                    emergencyName = mBinding.etEmergencyName.text?.toString()
                    emergencyNumber = mBinding.etEmergencyPhoneNumber.text?.toString()
                    name = mBinding.etNickName.text?.toString()
                    password = mBinding.etAccountPwd.text?.toString()
                }

                mViewModel.register(
                    userInfo = UserInfo.instance,
                    mBinding.etVerificationCode.text.toString()
                ) {
                    // 获取userSig函数
                    val userSig =
                        GenerateTestUserSig.genTestUserSig(mBinding.etPhoneNum.text.toString())
                    UserInfo.instance.userSig = userSig
                    TUIUtils.login(
                        mBinding.etPhoneNum.text.toString(),
                        userSig,
                        object : V2TIMCallback {
                            override fun onError(code: Int, desc: String) {
                                runOnUiThread { ToastUtil.toastLongMessage(getString(R.string.failed_login_tip) + ", errCode = " + code + ", errInfo = " + desc) }
                                DemoLog.i(
                                    TAG,
                                    "imRegister errorCode = $code, errorInfo = $desc"
                                )
                            }

                            override fun onSuccess() {
                                UserInfo.instance.isAutoLogin = true
                                val intent =
                                    Intent(this@RegisterForDevActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        })
                }
            }
            mBinding.btnGetVerifyCode.id -> {

            }
        }
    }

}