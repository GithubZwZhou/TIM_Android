package com.tencent.qcloud.tim.demo.chat

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.SyncStateContract
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.databinding.FragmentChatBinding
import com.tencent.qcloud.tuicore.TUIConfig
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.component.TitleBarLayout
import com.tencent.qcloud.tuicore.component.fragments.BaseFragment
import com.tencent.qcloud.tuicore.component.interfaces.IUIKitCallback
import com.tencent.qcloud.tuikit.tuichat.TUIChatConstants
import com.tencent.qcloud.tuikit.tuichat.bean.ChatInfo
import com.tencent.qcloud.tuikit.tuichat.bean.message.CallingMessageBean
import com.tencent.qcloud.tuikit.tuichat.bean.message.TUIMessageBean
import com.tencent.qcloud.tuikit.tuichat.bean.message.TextMessageBean
import com.tencent.qcloud.tuikit.tuichat.component.AudioPlayer
import com.tencent.qcloud.tuikit.tuichat.presenter.ChatPresenter
import com.tencent.qcloud.tuikit.tuichat.ui.interfaces.OnItemClickListener
import com.tencent.qcloud.tuikit.tuichat.ui.view.ChatView
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatLog
import com.tencent.qcloud.tuikit.tuichat.util.TUIChatUtils
import kotlin.reflect.KClass

open class MyTUIBaseChatFragment : BaseFragment() {
    protected lateinit var mBinding: FragmentChatBinding
    protected lateinit var mTitleBar: TitleBarLayout
    private var mForwardSelectMsgInfos: List<TUIMessageBean>? = null
    private var mForwardMode = 0

    private var mPresenter: ChatPresenter? = null
    protected open var mChatInfo: ChatInfo? = null
    protected lateinit var mHelper: ChatLayoutSetting

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        TUIChatLog.i(TAG, "oncreate view $this")
        mBinding = FragmentChatBinding.inflate(inflater)
        val bundle = arguments ?: return mBinding.root

        var startTime: Long = 0
        mChatInfo?.let {
            startTime =
                requireActivity().getSharedPreferences(CONVERSATION_DURATION, Context.MODE_PRIVATE)
                    .getLong(it.id, System.currentTimeMillis())
            requireActivity().getSharedPreferences(CONVERSATION_DURATION, Context.MODE_PRIVATE).edit {
                putLong(it.id, startTime)
            }
            // 这里需要判断是否在等待中 true
            if (startTime <= 0 && false) {
                disableConversation()
            }
        }
        Log.e("ChatFrag", "get: chatId: ${mChatInfo?.id}, startTime: $startTime")

        mHelper = ChatLayoutSetting()
        mHelper.setGroupId(mChatInfo?.id)
        mHelper.setStartTime(startTime)
        mHelper.customizeChatLayout(mBinding.chatLayout)
        return mBinding.root
    }

    protected open fun initView() {
        //单聊组件的默认UI和交互初始化
        mBinding.chatLayout.initDefault()

        //获取单聊面板的标题栏
        mTitleBar = mBinding.chatLayout.titleBar

        //单聊面板标记栏返回按钮点击事件，这里需要开发者自行控制
        mTitleBar.setOnLeftClickListener { requireActivity().finish() }
        mBinding.chatLayout.setForwardSelectActivityListener { mode, msgIds ->
            mForwardMode = mode
            mForwardSelectMsgInfos = msgIds
            val bundle = Bundle()
            bundle.putInt(TUIChatConstants.FORWARD_MODE, mode)
            TUICore.startActivity(
                this@MyTUIBaseChatFragment,
                "TUIForwardSelectActivity",
                bundle,
                TUIChatConstants.FORWARD_SELECT_ACTIVTY_CODE
            )
        }
        mBinding.chatLayout.messageLayout.onItemClickListener = object : OnItemClickListener {
            override fun onMessageLongClick(view: View, position: Int, message: TUIMessageBean) {
                //因为adapter中第一条为加载条目，位置需减1
                TUIChatLog.d(TAG, "chatfragment onTextSelected selectedText = ")
                mBinding.chatLayout.messageLayout.showItemPopMenu(position - 1, message, view)
            }

            override fun onUserIconClick(view: View, position: Int, message: TUIMessageBean) {
                val bundle = Bundle()
                bundle.putString("chatId", message.sender)
                TUICore.startActivity("FriendProfileActivity", bundle)
            }

            override fun onUserIconLongClick(view: View, position: Int, message: TUIMessageBean) {}

            override fun onReEditRevokeMessage(
                view: View,
                position: Int,
                messageInfo: TUIMessageBean
            ) {
                val messageType = messageInfo.msgType
                if (messageType == V2TIMMessage.V2TIM_ELEM_TYPE_TEXT) {
                    mBinding.chatLayout.inputLayout.appendText(messageInfo.v2TIMMessage.textElem.text)
                } else {
                    TUIChatLog.e(TAG, "error type: $messageType")
                }
            }

            override fun onRecallClick(view: View, position: Int, messageInfo: TUIMessageBean) {
                val callingMessageBean = messageInfo as CallingMessageBean
                val map: MutableMap<String, Any> = HashMap()
                map[TUIConstants.TUICalling.PARAM_NAME_USERIDS] = arrayOf(messageInfo.getUserId())
                map[TUIConstants.TUICalling.PARAM_NAME_TYPE] = when (callingMessageBean.callType) {
                    CallingMessageBean.ACTION_ID_VIDEO_CALL -> {
                        TUIConstants.TUICalling.TYPE_VIDEO
                    }
                    CallingMessageBean.ACTION_ID_AUDIO_CALL -> {
                        TUIConstants.TUICalling.TYPE_AUDIO
                    }
                    else -> ""
                }
                TUICore.callService(
                    TUIConstants.TUICalling.SERVICE_NAME,
                    TUIConstants.TUICalling.METHOD_NAME_CALL,
                    map
                )
            }

            override fun onTextSelected(view: View, position: Int, messageInfo: TUIMessageBean) {
                if (messageInfo is TextMessageBean) {
                    TUIChatLog.d(
                        TAG,
                        "chatfragment onTextSelected selectedText = ${messageInfo.selectText}"
                    )
                }
                mBinding.chatLayout.messageLayout.selectedPosition = position
                mBinding.chatLayout.messageLayout.showItemPopMenu(position - 1, messageInfo, view)
            }
        }
        mBinding.chatLayout.inputLayout.setStartActivityListener {
            val param = Bundle()
            param.putString(TUIChatConstants.GROUP_ID, mChatInfo!!.id)
            TUICore.startActivity(
                this@MyTUIBaseChatFragment,
                "StartGroupMemberSelectActivity",
                param,
                1
            )
        }
    }

    private fun getLastMsgTime(): Long {
        val clazz: Class<ChatView> = ChatView::class.java
        mBinding.chatLayout.getConversationLastMessage(mChatInfo?.id)
        val field = clazz.getDeclaredField("mConversationLastMessage")
        field.isAccessible = true
        val lastMsg = field
            .get(mBinding.chatLayout) as TUIMessageBean?
        Log.e(TAG, "lastMsg.msgTime: ${lastMsg?.messageTime}")
        return lastMsg?.messageTime ?: System.currentTimeMillis()
    }

    private fun disableConversation() {
        // TODO 禁用输入框等内容
        requireActivity().getSharedPreferences(CONVERSATION_DURATION, Context.MODE_PRIVATE).edit {
            if (mChatInfo == null) {
                return
            }
            remove(mChatInfo!!.id)
            Log.e("ChatFrag", "remove: charId: ${mChatInfo?.id}")
        }
    }

    @Suppress("unchecked_cast")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == 3) {
            val result_ids =
                data!!.getStringArrayListExtra(TUIChatConstants.Selection.USER_ID_SELECT)
            val result_names =
                data.getStringArrayListExtra(TUIChatConstants.Selection.USER_NAMECARD_SELECT)
            mBinding.chatLayout.inputLayout.updateInputText(result_names, result_ids)
            return
        }

        if (requestCode == TUIChatConstants.FORWARD_SELECT_ACTIVTY_CODE && resultCode == TUIChatConstants.FORWARD_SELECT_ACTIVTY_CODE) {
            if (data == null || mForwardSelectMsgInfos.isNullOrEmpty()) {
                return
            }
            val chatMap =
                data.getSerializableExtra(TUIChatConstants.FORWARD_SELECT_CONVERSATION_KEY) as HashMap<String, Boolean>?
            if (chatMap.isNullOrEmpty()) {
                return
            }
            for ((id, isGroup) in chatMap) { //遍历发送对象会话
                val chatInfo = mChatInfo ?: return
                val title = if (TUIChatUtils.isGroupChat(chatInfo.type)) {
                    getString(R.string.forward_chats)
                } else {
                    val userNickName = TUIConfig.getSelfNickName()
                    val senderName = if (!TextUtils.isEmpty(userNickName)) {
                        userNickName
                    } else {
                        TUILogin.getLoginUser()
                    }
                    val chatName: String = if (!TextUtils.isEmpty(chatInfo.chatName)) {
                        chatInfo.chatName
                    } else {
                        chatInfo.id
                    }
                    senderName + getString(R.string.and_text) + chatName + getString(R.string.forward_chats_c2c)
                }
                val selfConversation = id == chatInfo.id
                mPresenter!!.forwardMessage(mForwardSelectMsgInfos,
                    isGroup,
                    id,
                    title,
                    mForwardMode,
                    selfConversation,
                    object : IUIKitCallback<Any> {
                        override fun onSuccess(data: Any) {
                            TUIChatLog.v(TAG, "sendMessage onSuccess:")
                        }

                        override fun onError(module: String, errCode: Int, errMsg: String) {
                            TUIChatLog.v(
                                TAG,
                                "sendMessage fail:$errCode=$errMsg"
                            )
                        }
                    })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mPresenter?.isChatFragmentShow = true
        mHelper.setLastMsgTime(getLastMsgTime())
        mHelper.startChronometer()
    }

    override fun onPause() {
        super.onPause()
        val duringTime = mHelper.stopChronometer()
        requireActivity().getSharedPreferences(CONVERSATION_DURATION, Context.MODE_PRIVATE).edit {
            if (mChatInfo == null) {
                return
            }
//            putLong(mChatInfo!!.id, System.currentTimeMillis() - duringTime)
            Log.e("ChatFrag", "store: charId: ${mChatInfo?.id}, duringTime: $duringTime")
        }
        mBinding.chatLayout.inputLayout?.setDraft()
        mPresenter?.isChatFragmentShow = false
        AudioPlayer.getInstance().stopPlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.chatLayout.exitChat()
    }

    companion object {
        private const val TAG: String = "MyTUIBaseChatFragment"
        const val CONVERSATION_DURATION = "conversationStart"
    }
}