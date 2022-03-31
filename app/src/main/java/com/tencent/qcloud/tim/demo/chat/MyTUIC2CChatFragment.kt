package com.tencent.qcloud.tim.demo.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuikit.tuichat.TUIChatConstants
import com.tencent.qcloud.tuikit.tuichat.bean.ChatInfo
import com.tencent.qcloud.tuikit.tuichat.presenter.C2CChatPresenter
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatLog

class MyTUIC2CChatFragment : MyTUIBaseChatFragment() {
    private var mPresenter: C2CChatPresenter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        TUIChatLog.i(TAG, "oncreate view $this")
        mChatInfo = (arguments?.getSerializable(TUIChatConstants.CHAT_INFO) as ChatInfo?)
        super.onCreateView(inflater, container, savedInstanceState)

        mChatInfo?.let {
            initView()
        }

        return mBinding.root
    }

    override fun initView() {
        super.initView()
        // TODO 这里的入口是完成评价的界面
        mTitleBar.setOnRightClickListener {
            val intent = Intent(requireActivity(), ChatFinishActivity::class.java)
            intent.putExtra("chatId", mChatInfo!!.id)
            startActivity(intent)
        }
        mBinding.chatLayout.setPresenter(mPresenter)
        mPresenter!!.chatInfo = mChatInfo
        mBinding.chatLayout.chatInfo = mChatInfo
    }

    fun setPresenter(presenter: C2CChatPresenter) {
        this.mPresenter = presenter
    }


    fun getChatInfo(): ChatInfo {
        return mChatInfo!!
    }

    companion object {
        private val TAG = MyTUIC2CChatFragment::class.java.simpleName
    }
}