package com.tencent.qcloud.tim.demo.chat

import android.os.Bundle
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.qcloud.tuikit.tuichat.TUIChatConstants
import com.tencent.qcloud.tuikit.tuichat.bean.ChatInfo
import com.tencent.qcloud.tuikit.tuichat.presenter.C2CChatPresenter
import com.tencent.qcloud.tuikit.tuichat.ui.page.TUIBaseChatActivity
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatLog
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatUtils

class MyTUIC2CChatActivity : TUIBaseChatActivity() {
    private lateinit var mChatFragment: MyTUIC2CChatFragment
    private lateinit var presenter: C2CChatPresenter

    override fun initChat(chatInfo: ChatInfo) {
        TUIChatLog.i(TAG, "inti chat $chatInfo")
        if (!TUIChatUtils.isC2CChat(chatInfo.type)) {
            TUIChatLog.e(TAG, "init C2C chat failed , chatInfo = $chatInfo")
            ToastUtil.toastShortMessage("init c2c chat failed.")
        }
        mChatFragment = MyTUIC2CChatFragment()
        val bundle = Bundle()
        bundle.putSerializable(TUIChatConstants.CHAT_INFO, chatInfo)
        mChatFragment.arguments = bundle
        presenter = C2CChatPresenter()
        presenter.initListener()
        mChatFragment.setPresenter(presenter)
        supportFragmentManager.beginTransaction().replace(R.id.empty_view, mChatFragment)
            .commitAllowingStateLoss()
    }

    private companion object {
        val TAG: String = MyTUIC2CChatActivity::class.java.simpleName
    }
}