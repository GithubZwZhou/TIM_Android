package com.tencent.qcloud.tim.demo.chat

import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.tencent.imsdk.v2.V2TIMGroupApplication
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.databinding.ActivityChatFinishBinding
import com.tencent.qcloud.tuikit.tuicontact.TUIContactConstants
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactGroupApplyInfo
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactItemBean
import com.tencent.qcloud.tuikit.tuicontact.presenter.FriendProfilePresenter
import com.tencent.qcloud.tuikit.tuicontact.ui.view.FriendProfileLayout
import com.tencent.qcloud.tuikit.tuicontact.ui.view.FriendProfileLayout.OnButtonClickListener
import com.tencent.qcloud.tuikit.tuicontact.util.ContactUtils

class ChatFinishActivity: AppCompatActivity() {
    private lateinit var mBinding: ActivityChatFinishBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityChatFinishBinding.inflate(layoutInflater)
    }
}