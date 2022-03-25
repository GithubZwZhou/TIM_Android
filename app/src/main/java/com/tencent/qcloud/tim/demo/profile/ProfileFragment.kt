package com.tencent.qcloud.tim.demo.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.bean.UserInfo.Companion.instance
import com.tencent.qcloud.tim.demo.utils.TUIKitConstants
import com.tencent.qcloud.tim.demo.utils.TUIUtils.logout
import com.tencent.qcloud.tim.demo.utils.TUIUtils.startActivity
import com.tencent.qcloud.tuicore.component.dialog.TUIKitDialog
import com.tencent.qcloud.tuicore.component.fragments.BaseFragment
import com.tencent.qcloud.tuicore.util.ToastUtil

class ProfileFragment : BaseFragment() {
    private lateinit var mBaseView: View
    private lateinit var mProfileLayout: ProfileLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBaseView = inflater.inflate(R.layout.profile_fragment, container, false)
        initView()
        return mBaseView
    }

    private fun initView() {
        mProfileLayout = mBaseView.findViewById(R.id.profile_view)
        mBaseView.findViewById<View>(R.id.logout_btn).setOnClickListener {
            TUIKitDialog(activity)
                .builder()
                .setCancelable(true)
                .setCancelOutside(true)
                .setTitle(getString(R.string.logout_tip))
                .setDialogWidth(0.75f)
                .setPositiveButton(getString(R.string.sure)) {
                    val logoutThread = Thread {
                        logout(object : V2TIMCallback {
                            override fun onSuccess() {}
                            override fun onError(code: Int, desc: String) {
                                ToastUtil.toastLongMessage("logout fail: $code=$desc")
                            }
                        })
                    }
                    logoutThread.name = "Logout-Thread"
                    logoutThread.start()
                    instance.token = ""
                    instance.isAutoLogin = false
                    val bundle = Bundle()
                    bundle.putBoolean(TUIKitConstants.LOGOUT, true)
                    startActivity("LoginForDevActivity", bundle)
                    if (activity != null) {
                        requireActivity().finish()
                    }
                }
                .setNegativeButton(getString(R.string.cancel)) { }
                .show()
        }
    }
}