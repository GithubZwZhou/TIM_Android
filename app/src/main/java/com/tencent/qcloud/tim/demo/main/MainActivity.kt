package com.tencent.qcloud.tim.demo.main

//import com.tencent.qcloud.tim.demo.contact.MyTUIContactFragment
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.tencent.imsdk.v2.*
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.SplashActivity
import com.tencent.qcloud.tim.demo.bean.CallModel
import com.tencent.qcloud.tim.demo.bean.OfflineMessageBean
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.contact.MyTUIContactFragment
import com.tencent.qcloud.tim.demo.conversation.MyTUIConversationFragment
import com.tencent.qcloud.tim.demo.databinding.MainActivityBinding
import com.tencent.qcloud.tim.demo.profile.ProfileFragment
import com.tencent.qcloud.tim.demo.thirdpush.OEMPush.HUAWEIHmsMessageService
import com.tencent.qcloud.tim.demo.thirdpush.OfflineMessageDispatcher
import com.tencent.qcloud.tim.demo.thirdpush.PushSetting
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.TUIUtils
import com.tencent.qcloud.tuicore.TUIThemeManager
import com.tencent.qcloud.tuicore.component.action.PopActionClickListener
import com.tencent.qcloud.tuicore.component.action.PopMenuAction
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.component.interfaces.ITitleBarLayout
import com.tencent.qcloud.tuicore.component.menu.Menu
import com.tencent.qcloud.tuicore.util.ErrorMessageConverter
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.qcloud.tuikit.tuicontact.TUIContactConstants
import com.tencent.qcloud.tuikit.tuiconversation.TUIConversationConstants
import kotlin.math.abs

class MainActivity : BaseLightActivity() {
    private lateinit var mBinding: MainActivityBinding
    private var mLastTab: View? = null
    private var menu: Menu? = null
    private val mFragments: List<Fragment> by lazy {
        listOf(
            MyTUIConversationFragment(),
            MyTUIContactFragment(),
            ProfileFragment()
        )
    }
//    private val count = 0
//    private val lastClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        DemoLog.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        DemoApplication.mApplication!!.initPush() // 注册离线推送服务
        DemoApplication.mApplication!!.bindUserID(UserInfo.instance.userId ?: "") // 绑定离线推送的账号
        initView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DemoLog.i(TAG, "onNewIntent")
        setIntent(intent)
        DemoApplication.mApplication!!.initPush()
        DemoApplication.mApplication!!.bindUserID(UserInfo.instance.userId ?: "")
    }

    /**
     * 初始化活动的页面
     */
    private fun initView() {
        mBinding = MainActivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initMenuAction()

        val fragmentAdapter = FragmentAdapter(this)
        fragmentAdapter.setFragmentList(mFragments)

        mBinding.viewPager.apply {
            // 关闭左右滑动切换页面
            isUserInputEnabled = false
            // 设置缓存数量为4 避免销毁重建
            offscreenPageLimit = 4
            adapter = fragmentAdapter
            setCurrentItem(0, false)
        }

        setConversationTitleBar()

        if (mLastTab == null) {
            mLastTab = mBinding.conversationBtnGroup
        } else {
            // 初始化时，强制切换tab到上一次的位置
            tabClick(mLastTab!!)
        }
        prepareToClearAllUnreadMessage()
    }

    /**
     * 初始化 MenuAction
     * TODO 确定具体的边框
     */
    private fun initMenuAction() {
        val titleBarIconSize = resources.getDimensionPixelSize(R.dimen.demo_title_bar_icon_size)
        mBinding.mainTitleBar.apply {
            leftIcon.maxHeight = titleBarIconSize
            leftIcon.maxWidth = titleBarIconSize
            rightIcon.maxHeight = titleBarIconSize
            rightIcon.maxWidth = titleBarIconSize
            setOnRightClickListener(View.OnClickListener {
                if (menu == null) {
                    return@OnClickListener
                }
                if (menu!!.isShowing) {
                    menu!!.hide()
                } else {
                    menu!!.show()
                }
            })
        }
    }

    /**
     * 手指滑动超过一定像素时, 删除未读消息
     * TODO: 解决 与 onClick 点击事件之间的冲突
     */
    @Suppress("ClickableViewAccessibility")
    private fun prepareToClearAllUnreadMessage() {
        mBinding.msgTotalUnread.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var isTriggered = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = mBinding.msgTotalUnread.x
                        downY = mBinding.msgTotalUnread.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isTriggered) {
                            return true
                        }
                        val viewX = view.x
                        val viewY = view.y
                        val eventX = event.x
                        val eventY = event.y
                        val translationX = eventX + viewX - downX
                        val translationY = eventY + viewY - downY
                        view.translationX = translationX
                        view.translationY = translationY
                        // 移动的 x 和 y 轴坐标超过一定像素则触发一键清空所有会话未读
                        if (abs(translationX) > 200 || abs(translationY) > 200) {
                            isTriggered = true
                            mBinding.msgTotalUnread.visibility = View.GONE
                            triggerClearAllUnreadMessage()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        view.translationX = 0f
                        view.translationY = 0f
                        isTriggered = false
                    }
                    MotionEvent.ACTION_CANCEL -> isTriggered = false
                }
                return true
            }
        })
    }

    /**
     * 删除 未读提示, 并 Toast。
     */
    private fun triggerClearAllUnreadMessage() {
        V2TIMManager.getMessageManager().markAllMessageAsRead(object : V2TIMCallback {
            override fun onSuccess() {
                Log.i(TAG, "markAllMessageAsRead success")
                ToastUtil.toastShortMessage(this@MainActivity.getString(R.string.mark_all_message_as_read_succ))
            }

            override fun onError(code: Int, desc: String) {
                Log.i(
                    TAG,
                    "markAllMessageAsRead error:$code, desc:"
                            + ErrorMessageConverter.convertIMError(code, desc)
                )
                ToastUtil.toastShortMessage(
                    this@MainActivity.getString(
                        R.string.mark_all_message_as_read_err_format,
                        code,
                        ErrorMessageConverter.convertIMError(code, desc)
                    )
                )
                mBinding.msgTotalUnread.visibility = View.VISIBLE
            }
        })
    }

    /**
     * 切换页面:
     * 1. 修改 [mLastTab]
     * 2. 重置 所有页 底部图片和文字 的颜色
     * 2. ViewPager 切换至 当前页面
     * 3. 修改顶部 TitleBar
     * 4. 设置 当前页 底部图片和文字 的颜色
     * TODO: 优化, 减少无关页面的颜色替换
     */
    fun tabClick(view: View) {
        DemoLog.i(TAG, "tabClick last: $mLastTab current: $view")
        if (mLastTab?.id == view.id) {
            return
        }
        mLastTab = view
        resetMenuState()
        when (view.id) {
            R.id.conversation_btn_group -> {
                mBinding.viewPager.setCurrentItem(0, false)
                setConversationTitleBar()
                mBinding.conversation.setTextColor(
                    getCompatColor(R.attr.demo_main_tab_selected_text_color)
                )
                mBinding.tabConversationIcon.background =
                    getCompatDrawable(R.attr.demo_main_tab_conversation_selected_bg)
            }
            R.id.contact_btn_group -> {
                mBinding.viewPager.setCurrentItem(1, false)
                setContactTitleBar()
                mBinding.contact.setTextColor(
                    getCompatColor(R.attr.demo_main_tab_selected_text_color)
                )
                mBinding.tabContactIcon.background =
                    getCompatDrawable(R.attr.demo_main_tab_contact_selected_bg)
            }
            R.id.myself_btn_group -> {
                mBinding.viewPager.setCurrentItem(2, false)
                setProfileTitleBar()
                mBinding.mine.setTextColor(
                    getCompatColor(R.attr.demo_main_tab_selected_text_color)
                )
                mBinding.tabProfileIcon.background =
                    getCompatDrawable(R.attr.demo_main_tab_profile_selected_bg)
            }
            else -> {}
        }
    }

    /**
     * 消息页面 Title 的设置
     */
    private fun setConversationTitleBar() {
        mBinding.mainTitleBar.apply {
            this.setTitle(
                resources.getString(R.string.conversation_title), // 腾讯·云通信
                ITitleBarLayout.Position.MIDDLE
            )
            this.leftGroup.visibility = View.GONE
            this.rightGroup.visibility = View.VISIBLE
            this.setRightIcon(
                TUIThemeManager.getAttrResId(
                    this@MainActivity,
                    R.attr.demo_title_bar_more
                )
            )
        }
        setConversationMenu()
    }

    /**
     * 设置 消息页 右边的 Menu (+)
     * TODO 优化/简化
     */
    private fun setConversationMenu() {
        menu = Menu(this, mBinding.mainTitleBar.rightIcon)

        val popActionClickListener = PopActionClickListener { _, data ->
            val action = data as PopMenuAction
            when (action.actionName) {
                resources.getString(R.string.start_conversation) -> {
                    TUIUtils.startActivity("StartC2CChatActivity", null)
                }
                resources.getString(R.string.create_private_group) -> {
                    val bundle = Bundle()
                    bundle.putInt(
                        TUIConversationConstants.GroupType.TYPE,
                        TUIConversationConstants.GroupType.PRIVATE
                    )
                    TUIUtils.startActivity("StartGroupChatActivity", bundle)
                }
                resources.getString(R.string.create_group_chat) -> {
                    val bundle = Bundle()
                    bundle.putInt(
                        TUIConversationConstants.GroupType.TYPE,
                        TUIConversationConstants.GroupType.PUBLIC
                    )
                    TUIUtils.startActivity("StartGroupChatActivity", bundle)
                }
                resources.getString(R.string.create_chat_room) -> {
                    val bundle = Bundle()
                    bundle.putInt(
                        TUIConversationConstants.GroupType.TYPE,
                        TUIConversationConstants.GroupType.CHAT_ROOM
                    )
                    TUIUtils.startActivity("StartGroupChatActivity", bundle)
                }
                resources.getString(R.string.create_community) -> {
                    val bundle = Bundle()
                    bundle.putInt(
                        TUIConversationConstants.GroupType.TYPE,
                        TUIConversationConstants.GroupType.COMMUNITY
                    )
                    TUIUtils.startActivity("StartGroupChatActivity", bundle)
                }
            }

            menu!!.hide()
        }

        // 设置右上角+号显示PopAction
        val menuActions = listOf(
            PopMenuAction().apply {
                actionName = resources.getString(R.string.start_conversation)
                actionClickListener = popActionClickListener
                iconResId = R.drawable.create_c2c
            },
            PopMenuAction().apply {
                actionName = resources.getString(R.string.create_private_group)
                iconResId = R.drawable.group_icon
                actionClickListener = popActionClickListener
            },
            PopMenuAction().apply {
                actionName = resources.getString(R.string.create_private_group)
                iconResId = R.drawable.group_icon
                actionClickListener = popActionClickListener
            },
            PopMenuAction().apply {
                actionName = resources.getString(R.string.create_group_chat)
                iconResId = R.drawable.group_icon
                actionClickListener = popActionClickListener
            },
            PopMenuAction().apply {
                actionName = resources.getString(R.string.create_chat_room)
                iconResId = R.drawable.group_icon
                actionClickListener = popActionClickListener
            },
            PopMenuAction().apply {
                actionName = resources.getString(R.string.create_community)
                iconResId = R.drawable.group_icon
                actionClickListener = popActionClickListener
            }
        )
        menu!!.setMenuAction(menuActions)
    }

    /**
     * 通讯录页面 Title 的设置
     */
    private fun setContactTitleBar() {
        mBinding.mainTitleBar.apply {
            setTitle(
                resources.getString(R.string.contact_title), // 通讯录
                ITitleBarLayout.Position.MIDDLE
            )
            leftGroup.visibility = View.GONE
            rightGroup.visibility = View.VISIBLE
            setRightIcon(
                TUIThemeManager.getAttrResId(
                    this@MainActivity,
                    R.attr.demo_title_bar_more
                )
            )
        }
        setContactMenu()
    }

    /**
     * 设置 通讯录页 右边的 Menu (+)
     * TODO 优化/简化
     */
    private fun setContactMenu() {
        menu = Menu(this, mBinding.mainTitleBar.rightIcon)
        val popActionClickListener = PopActionClickListener { _, data ->
            val action = data as PopMenuAction
            when (action.actionName) {
                resources.getString(R.string.add_friend) -> {
                    val bundle = Bundle()
                    bundle.putBoolean(TUIContactConstants.GroupType.GROUP, false)
                    TUIUtils.startActivity("AddMoreActivity", bundle)
                }
                resources.getString(R.string.add_group) -> {
                    val bundle = Bundle()
                    bundle.putBoolean(TUIContactConstants.GroupType.GROUP, true)
                    TUIUtils.startActivity("AddMoreActivity", bundle)
                }
            }
            menu!!.hide()
        }
        val menuActions: List<PopMenuAction> = listOf(
            PopMenuAction().apply {
                actionName = resources.getString(R.string.add_friend)
                iconResId = R.drawable.demo_add_friend
                actionClickListener = popActionClickListener
            },
            PopMenuAction().apply {
                actionName = resources.getString(R.string.add_group)
                iconResId = R.drawable.demo_add_group
                actionClickListener = popActionClickListener
            }
        )
        menu!!.setMenuAction(menuActions)
    }

    /**
     * 个人页面 Title 的设置
     */
    private fun setProfileTitleBar() {
        mBinding.mainTitleBar.apply {
            leftGroup.visibility = View.GONE
            rightGroup.visibility = View.GONE
            setTitle(
                resources.getString(R.string.profile), // 我
                ITitleBarLayout.Position.MIDDLE
            )
        }
    }

    /**
     * 重置 所有页 底部图片和文字 的颜色
     */
    private fun resetMenuState() {
        mBinding.conversation.setTextColor(
            getCompatColor(R.attr.demo_main_tab_normal_text_color)
        )
        mBinding.contact.setTextColor(
            getCompatColor(R.attr.demo_main_tab_normal_text_color)
        )
        mBinding.mine.setTextColor(
            getCompatColor(R.attr.demo_main_tab_normal_text_color)
        )

        mBinding.tabConversationIcon.background =
            getCompatDrawable(R.attr.demo_main_tab_conversation_normal_bg)
        mBinding.tabContactIcon.background =
            getCompatDrawable(R.attr.demo_main_tab_contact_normal_bg)
        mBinding.tabProfileIcon.background =
            getCompatDrawable(R.attr.demo_main_tab_profile_normal_bg)
    }

    /**
     * 消息页 未读数监听
     * 需要在 UI 线程中运行
     */
    private val unreadListener: V2TIMConversationListener = object : V2TIMConversationListener() {
        override fun onTotalUnreadMessageCountChanged(totalUnreadCount: Long) {
            if (totalUnreadCount > 0) {
                mBinding.msgTotalUnread.visibility = View.VISIBLE
            } else {
                mBinding.msgTotalUnread.visibility = View.GONE
            }
            var unreadStr = "" + totalUnreadCount
            if (totalUnreadCount > 100) {
                unreadStr = "99+"
            }
            mBinding.msgTotalUnread.text = unreadStr
            // 华为离线推送角标
            HUAWEIHmsMessageService.updateBadge(this@MainActivity, totalUnreadCount.toInt())
        }
    }

    /**
     * 通讯录下方的消息未读数监听
     */
    private val friendshipListener: V2TIMFriendshipListener = object : V2TIMFriendshipListener() {
        override fun onFriendApplicationListAdded(applicationList: List<V2TIMFriendApplication>) {
            refreshFriendApplicationUnreadCount()
        }

        override fun onFriendApplicationListDeleted(userIDList: List<String>) {
            refreshFriendApplicationUnreadCount()
        }

        override fun onFriendApplicationListRead() {
            refreshFriendApplicationUnreadCount()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 注册监听器，如果是通过状态栏进入，则需要进入相应的对话页面
     */
    override fun onResume() {
        DemoLog.i(TAG, "onResume")
        super.onResume()
        registerUnreadListener()
        handleOfflinePush()
    }

    /**
     * 注册 [unreadListener] 监听 对话
     * 在未读消息变化时回调 [onTotalUnreadMessageCountChanged#unreadListener]
     * 注册 [friendshipListener] 监听 通讯录(新增朋友)
     * 修改 下方通讯录 Tab 的未读消息数
     */
    private fun registerUnreadListener() {
        V2TIMManager.getConversationManager().addConversationListener(unreadListener)
        V2TIMManager.getConversationManager()
            .getTotalUnreadMessageCount(object : V2TIMValueCallback<Long> {
                override fun onSuccess(aLong: Long) {
                    runOnUiThread { unreadListener.onTotalUnreadMessageCountChanged(aLong) }
                }

                override fun onError(code: Int, desc: String) {}
            })
        V2TIMManager.getFriendshipManager().addFriendListener(friendshipListener)
        refreshFriendApplicationUnreadCount()
    }

    /**
     * 修改 下方通讯录 Tab 的未读消息数
     */
    private fun refreshFriendApplicationUnreadCount() {
        V2TIMManager.getFriendshipManager()
            .getFriendApplicationList(object : V2TIMValueCallback<V2TIMFriendApplicationResult> {
                override fun onSuccess(v2TIMFriendApplicationResult: V2TIMFriendApplicationResult) {
                    runOnUiThread {
                        val unreadCount = v2TIMFriendApplicationResult.unreadCount
                        if (unreadCount > 0) {
                            mBinding.newFriendTotalUnread.visibility = View.VISIBLE
                        } else {
                            mBinding.newFriendTotalUnread.visibility = View.GONE
                        }
                        var unreadStr = "" + unreadCount
                        if (unreadCount > 100) {
                            unreadStr = "99+"
                        }
                        mBinding.newFriendTotalUnread.text = unreadStr
                    }
                }

                override fun onError(code: Int, desc: String) {}
            })
    }

    /**
     * 处理离线推送(点击状态栏进入的处理)
     */
    private fun handleOfflinePush() {
        // 当前状态为退出状态
        if (V2TIMManager.getInstance().loginStatus == V2TIMManager.V2TIM_STATUS_LOGOUT) {
            val newIntent = Intent(this@MainActivity, SplashActivity::class.java)
            if (intent != null) {
                // 厂商 和 TPNS 俩种推送方式
                if (PushSetting.isTPNSChannel) {
                    newIntent.data = intent.data
                } else {
                    newIntent.putExtras(intent.extras!!)
                }
            }
            startActivity(newIntent)
            finish()
            return
        }
        val bean = OfflineMessageDispatcher.parseOfflineMessage(intent)
        if (bean != null) {
            intent = null
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancelAll()
            // 消息
            if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CHAT) {
                if (TextUtils.isEmpty(bean.sender)) {
                    return
                }
                TUIUtils.startChat(bean.sender, bean.nickname, bean.chatType)
            } else if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CALL) {
                // 视频/语音
                handleOfflinePushCall(bean)
            }
        }
    }

    /**
     * 处理离线推送(点击状态栏进入的处理-视频/语音)
     */
    private fun handleOfflinePushCall(bean: OfflineMessageBean) {
        val model = Gson().fromJson(bean.content, CallModel::class.java)
        DemoLog.i(TAG, "bean: $bean model: $model")
        if (model != null) {
            val timeout = V2TIMManager.getInstance().serverTime - bean.sendTime
            if (timeout >= model.timeout) {
                ToastUtil.toastLongMessage(DemoApplication.mApplication!!.getString(R.string.call_time_out))
            } else {
                TUIUtils.startCall(bean.sender, bean.content)
            }
        }
    }

    /**
     * 取消会话和通讯录下方监听器的监听
     */
    override fun onPause() {
        DemoLog.i(TAG, "onPause")
        super.onPause()
        V2TIMManager.getConversationManager().removeConversationListener(unreadListener)
        V2TIMManager.getFriendshipManager().removeFriendListener(friendshipListener)
    }

    /**
     * 取消 [mLastTab]
     */
    override fun onDestroy() {
        DemoLog.i(TAG, "onDestroy")
        mLastTab = null
        super.onDestroy()
    }

    /**
     * 获取 Drawable 资源
     */
    private fun getCompatDrawable(attrId: Int) = ResourcesCompat.getDrawable(
        resources, TUIThemeManager.getAttrResId(this, attrId), null
    )

    /**
     * 获取 Color 资源
     */
    private fun getCompatColor(attrId: Int) = ResourcesCompat.getColor(
        resources, TUIThemeManager.getAttrResId(this, attrId), null
    )

    override fun onStart() {
        DemoLog.i(TAG, "onStart")
        super.onStart()
    }

    override fun onStop() {
        DemoLog.i(TAG, "onStop")
        super.onStop()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

}