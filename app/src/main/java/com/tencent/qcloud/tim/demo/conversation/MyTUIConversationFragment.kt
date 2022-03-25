package com.tencent.qcloud.tim.demo.conversation

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.ListView
import android.widget.PopupWindow
import androidx.core.app.ActivityCompat
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.qcloud.tim.demo.chat.MyTUIC2CChatActivity
import com.tencent.qcloud.tim.demo.databinding.ConversationFragmentBinding
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuicore.component.action.PopActionClickListener
import com.tencent.qcloud.tuicore.component.action.PopDialogAdapter
import com.tencent.qcloud.tuicore.component.action.PopMenuAction
import com.tencent.qcloud.tuicore.component.fragments.BaseFragment
import com.tencent.qcloud.tuicore.component.interfaces.IUIKitCallback
import com.tencent.qcloud.tuicore.util.ScreenUtil
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.qcloud.tuikit.tuiconversation.R
import com.tencent.qcloud.tuikit.tuiconversation.bean.ConversationInfo
import com.tencent.qcloud.tuikit.tuiconversation.presenter.ConversationPresenter
import java.io.Serializable

class MyTUIConversationFragment : BaseFragment() {
    private lateinit var mBinding: ConversationFragmentBinding

    private var mConversationPopWindow: PopupWindow? = null
    private lateinit var mConversationPopActions: List<PopMenuAction>
    private var presenter: ConversationPresenter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = ConversationFragmentBinding.inflate(inflater)
        initView()
        return mBinding.root
    }

    private fun initView() {
        presenter = ConversationPresenter()
        presenter!!.setConversationListener()
        // 会话面板
        mBinding.conversationLayout.apply {
            setPresenter(presenter)
            // 会话列表面板的默认UI和交互初始化
            initDefault()
            // 通过API设置ConversataonLayout各种属性的样例，开发者可以打开注释，体验效果
//            ConversationLayoutSetting.customizeConversation(this)
            // 此处为demo的实现逻辑，更根据会话类型跳转到相关界面，开发者可根据自己的应用场景灵活实现
            conversationList.setOnItemClickListener { _, _, conversationInfo ->
                startChatActivity(conversationInfo)
            }
            conversationList.setOnItemLongClickListener { view, _, conversationInfo ->
                showItemPopMenu(view, conversationInfo)
            }
        }
        initPopMenuAction()
        restoreConversationItemBackground()
    }

    private fun restoreConversationItemBackground() {
        mBinding.conversationLayout.conversationList?.adapter?.let {
            if (it.isClick) {
                it.isClick = false
                it.notifyItemChanged(it.currentPosition)
            }
        }
    }

    private fun initPopMenuAction() {
        // 设置长按 conversation 显示 PopAction
        mConversationPopActions = ArrayList<PopMenuAction>().apply {
            addAll(listOf(
                PopMenuAction().apply {
                    actionName = resources.getString(R.string.chat_top)
                    actionClickListener = PopActionClickListener { _, data ->
                        mBinding.conversationLayout.setConversationTop(
                            data as ConversationInfo,
                            object : IUIKitCallback<Any> {
                                override fun onSuccess(data: Any) {}
                                override fun onError(
                                    module: String,
                                    errCode: Int,
                                    errMsg: String
                                ) {
                                    ToastUtil.toastLongMessage("$module, Error code = $errCode, desc = $errMsg")
                                }
                            })
                    }
                },
                PopMenuAction().apply {
                    actionName = resources.getString(R.string.chat_delete)
                    actionClickListener = PopActionClickListener { _, data ->
                        mBinding.conversationLayout.deleteConversation(
                            data as ConversationInfo
                        )
                    }
                },
                PopMenuAction().apply {
                    actionName = resources.getString(R.string.clear_conversation_message)
                    actionClickListener =
                        PopActionClickListener { _, data ->
                            mBinding.conversationLayout.clearConversationMessage(
                                data as ConversationInfo
                            )
                        }
                }
            ))
        }
    }

    /**
     * 长按会话item弹框
     * @param view 长按 view
     * @param conversationInfo 会话数据对象
     */
    private fun showItemPopMenu(view: View, conversationInfo: ConversationInfo) {
        if (mConversationPopActions.isEmpty())
            return
        val itemPop = LayoutInflater.from(activity)
            .inflate(R.layout.conversation_pop_menu_layout, null)
        val conversationPopList = itemPop.findViewById<ListView>(R.id.pop_menu_list)

        conversationPopList.setOnItemClickListener { _, _, position, _ ->
            val action = mConversationPopActions[position]
            action.actionClickListener?.onActionClick(position, conversationInfo)
            mConversationPopWindow!!.dismiss()
            restoreConversationItemBackground()
        }

        // 置顶聊天 / 取消置顶
        val chatTopStr = resources.getString(R.string.chat_top)
        val quitChatTopStr = resources.getString(R.string.quit_chat_top)
        mConversationPopActions.forEach { action ->
            when {
                conversationInfo.isTop && action.actionName == chatTopStr -> {
                    action.actionName = quitChatTopStr
                }
                !conversationInfo.isTop && action.actionName == quitChatTopStr -> {
                    action.actionName = chatTopStr
                }
            }
        }

        conversationPopList.adapter = PopDialogAdapter().apply {
            setDataSource(mConversationPopActions)
        }

        mConversationPopWindow = PopupWindow(
            itemPop,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            setBackgroundDrawable(ColorDrawable())
            isOutsideTouchable = true
            setOnDismissListener { restoreConversationItemBackground() }
        }

        val x = view.width / 2
        var y = -view.height / 3
        val popHeight = ScreenUtil.dip2px(45f) * 3
        if (y + popHeight + view.y + view.height > mBinding.conversationLayout.bottom) {
            y -= popHeight
        }
        mConversationPopWindow!!.showAsDropDown(view, x, y, Gravity.TOP or Gravity.START)
    }

    private fun startChatActivity(conversationInfo: ConversationInfo) {
        val param = Bundle()
        param.putInt(
            TUIConstants.TUIChat.CHAT_TYPE,
            if (conversationInfo.isGroup) V2TIMConversation.V2TIM_GROUP else V2TIMConversation.V2TIM_C2C
        )
        param.putString(TUIConstants.TUIChat.CHAT_ID, conversationInfo.id)
        param.putString(TUIConstants.TUIChat.CHAT_NAME, conversationInfo.title)

        if (conversationInfo.draft != null) {
            param.putString(TUIConstants.TUIChat.DRAFT_TEXT, conversationInfo.draft.draftText)
            param.putLong(TUIConstants.TUIChat.DRAFT_TIME, conversationInfo.draft.draftTime)
        }
        param.putBoolean(TUIConstants.TUIChat.IS_TOP_CHAT, conversationInfo.isTop)

        if (conversationInfo.isGroup) {
            param.putString(TUIConstants.TUIChat.FACE_URL, conversationInfo.iconPath)
            param.putString(TUIConstants.TUIChat.GROUP_TYPE, conversationInfo.groupType)
            param.putSerializable(
                TUIConstants.TUIChat.AT_INFO_LIST,
                conversationInfo.groupAtInfoList as Serializable
            )
            TUICore.startActivity(TUIConstants.TUIChat.GROUP_CHAT_ACTIVITY_NAME, param)
        } else {
            val intent = Intent(requireContext(), MyTUIC2CChatActivity::class.java)
            intent.putExtras(param)
            ActivityCompat.startActivity(requireContext(), intent, null)
//            TUICore.startActivity(MyTUIConstants.TUIChat.C2C_CHAT_ACTIVITY_NAME, param)
        }
    }
}