package com.tencent.qcloud.tim.demo.chat

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.databinding.ActivityChatFinishBinding
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.component.imageEngine.impl.GlideEngine
import com.tencent.qcloud.tuicore.component.interfaces.ITitleBarLayout
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatLog


class ChatFinishActivity : BaseLightActivity() {
    private lateinit var mBinding: ActivityChatFinishBinding
    private val mViewModel: ChatFinishViewModel by viewModels {
        ChatFinishViewModelProvider()
    }
    private var mStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        TUIChatLog.i(TAG, "init activity")
        super.onCreate(savedInstanceState)
        mBinding = ActivityChatFinishBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val chatId = intent.getStringExtra("chatId")
        Log.e(TAG, "chatId: $chatId")
        if (!chatId.isNullOrEmpty()) {
            mStartTime = getSharedPreferences(MyTUIBaseChatFragment.CONVERSATION_DURATION, Context.MODE_PRIVATE)
                .getLong(chatId, 0)
            mStartTime = (System.currentTimeMillis() - mStartTime) / 1000
            mStartTime /= 60
            mViewModel.getUserContactWrapperInfo(chatId)
        }

        mBinding.chatFinishTitlebar.setTitle("服务评价", ITitleBarLayout.Position.MIDDLE)

        mBinding.chatFinishTitlebar.leftIcon.setOnClickListener {
            finish()
        }

        registerObservers()
    }

    private fun registerObservers() {
        mViewModel.contactItemBean.observe(this) { itemWrapper ->
            mBinding.beanWrapper = itemWrapper
            mBinding.tvConversationCost.text = resources.getString(
                R.string.conversation_cost,
                itemWrapper.target,
                mStartTime.toString()
            )
            GlideEngine.loadUserIcon(
                mBinding.ivAvatar,
                itemWrapper.bean.avatarUrl,
                resources.getDimensionPixelSize(R.dimen.contact_profile_face_radius)
            )
            mBinding.notifyChange()
        }
        mViewModel.message.observe(this) {
            ToastUtil.toastShortMessage(it)
        }
    }

    companion object {
        private const val TAG = "ChatFinishActivity"
    }
}