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
        DemoApplication.mApplication!!.initPush() // ????????????????????????
        DemoApplication.mApplication!!.bindUserID(UserInfo.instance.userId ?: "") // ???????????????????????????
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
     * ????????????????????????
     */
    private fun initView() {
        mBinding = MainActivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initMenuAction()

        val fragmentAdapter = FragmentAdapter(this)
        fragmentAdapter.setFragmentList(mFragments)

        mBinding.viewPager.apply {
            // ??????????????????????????????
            isUserInputEnabled = false
            // ?????????????????????4 ??????????????????
            offscreenPageLimit = 4
            adapter = fragmentAdapter
            setCurrentItem(0, false)
        }

        setConversationTitleBar()

        if (mLastTab == null) {
            mLastTab = mBinding.conversationBtnGroup
        } else {
            // ???????????????????????????tab?????????????????????
            tabClick(mLastTab!!)
        }
        prepareToClearAllUnreadMessage()
    }

    /**
     * ????????? MenuAction
     * TODO ?????????????????????
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
     * ?????????????????????????????????, ??????????????????
     * TODO: ?????? ??? onClick ???????????????????????????
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
                        // ????????? x ??? y ??????????????????????????????????????????????????????????????????
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
     * ?????? ????????????, ??? Toast???
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
     * ????????????:
     * 1. ?????? [mLastTab]
     * 2. ?????? ????????? ????????????????????? ?????????
     * 2. ViewPager ????????? ????????????
     * 3. ???????????? TitleBar
     * 4. ?????? ????????? ????????????????????? ?????????
     * TODO: ??????, ?????????????????????????????????
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
     * ???????????? Title ?????????
     */
    private fun setConversationTitleBar() {
        mBinding.mainTitleBar.apply {
            this.setTitle(
                resources.getString(R.string.conversation_title), // ?????????????????
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
     * ?????? ????????? ????????? Menu (+)
     * TODO ??????/??????
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

        // ???????????????+?????????PopAction
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
     * ??????????????? Title ?????????
     */
    private fun setContactTitleBar() {
        mBinding.mainTitleBar.apply {
            setTitle(
                resources.getString(R.string.contact_title), // ?????????
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
     * ?????? ???????????? ????????? Menu (+)
     * TODO ??????/??????
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
     * ???????????? Title ?????????
     */
    private fun setProfileTitleBar() {
        mBinding.mainTitleBar.apply {
            leftGroup.visibility = View.GONE
            rightGroup.visibility = View.GONE
            setTitle(
                resources.getString(R.string.profile), // ???
                ITitleBarLayout.Position.MIDDLE
            )
        }
    }

    /**
     * ?????? ????????? ????????????????????? ?????????
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
     * ????????? ???????????????
     * ????????? UI ???????????????
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
            // ????????????????????????
            HUAWEIHmsMessageService.updateBadge(this@MainActivity, totalUnreadCount.toInt())
        }
    }

    /**
     * ???????????????????????????????????????
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
     * ???????????????????????????????????????????????????????????????????????????????????????
     */
    override fun onResume() {
        DemoLog.i(TAG, "onResume")
        super.onResume()
        registerUnreadListener()
        handleOfflinePush()
    }

    /**
     * ?????? [unreadListener] ?????? ??????
     * ?????????????????????????????? [onTotalUnreadMessageCountChanged#unreadListener]
     * ?????? [friendshipListener] ?????? ?????????(????????????)
     * ?????? ??????????????? Tab ??????????????????
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
     * ?????? ??????????????? Tab ??????????????????
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
     * ??????????????????(??????????????????????????????)
     */
    private fun handleOfflinePush() {
        // ???????????????????????????
        if (V2TIMManager.getInstance().loginStatus == V2TIMManager.V2TIM_STATUS_LOGOUT) {
            val newIntent = Intent(this@MainActivity, SplashActivity::class.java)
            if (intent != null) {
                // ?????? ??? TPNS ??????????????????
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
            // ??????
            if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CHAT) {
                if (TextUtils.isEmpty(bean.sender)) {
                    return
                }
                TUIUtils.startChat(bean.sender, bean.nickname, bean.chatType)
            } else if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CALL) {
                // ??????/??????
                handleOfflinePushCall(bean)
            }
        }
    }

    /**
     * ??????????????????(??????????????????????????????-??????/??????)
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
     * ????????????????????????????????????????????????
     */
    override fun onPause() {
        DemoLog.i(TAG, "onPause")
        super.onPause()
        V2TIMManager.getConversationManager().removeConversationListener(unreadListener)
        V2TIMManager.getFriendshipManager().removeFriendListener(friendshipListener)
    }

    /**
     * ?????? [mLastTab]
     */
    override fun onDestroy() {
        DemoLog.i(TAG, "onDestroy")
        mLastTab = null
        super.onDestroy()
    }

    /**
     * ?????? Drawable ??????
     */
    private fun getCompatDrawable(attrId: Int) = ResourcesCompat.getDrawable(
        resources, TUIThemeManager.getAttrResId(this, attrId), null
    )

    /**
     * ?????? Color ??????
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