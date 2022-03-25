package com.tencent.qcloud.tim.demo.contact

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.component.interfaces.IUIKitCallback
import com.tencent.qcloud.tuicore.util.BackgroundTasks
import com.tencent.qcloud.tuicore.util.ThreadHelper
import com.tencent.qcloud.tuikit.tuicontact.R
import com.tencent.qcloud.tuikit.tuicontact.TUIContactConstants
import com.tencent.qcloud.tuikit.tuicontact.TUIContactService
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactItemBean
import com.tencent.qcloud.tuikit.tuicontact.bean.FriendApplicationBean
import com.tencent.qcloud.tuikit.tuicontact.bean.GroupInfo
import com.tencent.qcloud.tuikit.tuicontact.bean.MessageCustom
import com.tencent.qcloud.tuikit.tuicontact.interfaces.ContactEventListener
import com.tencent.qcloud.tuikit.tuicontact.ui.interfaces.IContactListView
import com.tencent.qcloud.tuikit.tuicontact.ui.view.ContactListView
import com.tencent.qcloud.tuikit.tuicontact.util.ContactUtils
import com.tencent.qcloud.tuikit.tuicontact.util.TUIContactLog

class MyContactPresenter {
    private val provider: MyContactProvider = MyContactProvider()
    private var contactListView: IContactListView? = null
    private val dataSource: MutableList<ContactItemBean> = ArrayList()
    private lateinit var friendListListener: ContactEventListener
    private var blackListListener: ContactEventListener? = null
    private var isSelectForCall = false

    init {
        provider.nextSeq = 0
        setFriendListListener()
    }

    fun setContactListView(contactListView: IContactListView) {
        this.contactListView = contactListView
    }

    private fun setFriendListListener() {
        friendListListener = object : ContactEventListener() {
            override fun onFriendListAdded(users: List<ContactItemBean>) {
                onDataListAdd(users)
            }

            override fun onFriendListDeleted(userList: List<String>) {
                onDataListDeleted(userList)
            }

            override fun onFriendInfoChanged(infoList: List<ContactItemBean>) {
                this@MyContactPresenter.onFriendInfoChanged(infoList)
            }

            override fun onFriendRemarkChanged(id: String, remark: String) {
                this@MyContactPresenter.onFriendRemarkChanged(id, remark)
            }

            override fun onFriendApplicationListAdded(applicationList: List<FriendApplicationBean>) {
                contactListView?.onFriendApplicationChanged()
            }

            override fun onFriendApplicationListDeleted(userIDList: List<String>) {
                contactListView?.onFriendApplicationChanged()
            }
        }
        TUIContactService.getInstance().addContactEventListener(friendListListener)
    }

    fun setBlackListListener() {
        blackListListener = object : ContactEventListener() {
            override fun onBlackListAdd(infoList: List<ContactItemBean>) {
                onDataListAdd(infoList)
            }

            override fun onBlackListDeleted(userList: List<String>) {
                onDataListDeleted(userList)
            }
        }
        TUIContactService.getInstance().addContactEventListener(blackListListener)
    }

    fun setIsForCall(isSelectForCall: Boolean) {
        this.isSelectForCall = isSelectForCall
    }

    fun loadDataSource(dataSourceType: MyContactListView.DataSource) {
        val callback = object : IUIKitCallback<List<ContactItemBean>> {
            override fun onSuccess(data: List<ContactItemBean>) {
                TUIContactLog.i(TAG, "load data source success , loadType = $dataSourceType")
                onDataLoaded(data)
            }

            override fun onError(module: String, errCode: Int, errMsg: String) {
                TUIContactLog.e(
                    TAG, "load data source error , loadType = " + dataSourceType +
                            "  " + "errCode = " + errCode + "  errMsg = " + errMsg
                )
                onDataLoaded(ArrayList())
            }
        }
        dataSource.clear()
        when (dataSourceType) {
            MyContactListView.DataSource.FRIEND_LIST -> provider.loadFriendListDataAsync(callback)
            MyContactListView.DataSource.BLACK_LIST -> provider.loadBlackListData(callback)
            MyContactListView.DataSource.GROUP_LIST -> provider.loadGroupListData(callback)
            MyContactListView.DataSource.CONTACT_LIST -> {
//                dataSource.add(
//                    ContactItemBean(
//                        TUIContactService.getAppContext().resources.getString(
//                            R.string.new_friend
//                        )
//                    )
//                        .setTop(true)
//                        .setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP) as ContactItemBean
//                )
//                dataSource.add(
//                    ContactItemBean(
//                        TUIContactService.getAppContext().resources.getString(
//                            R.string.group
//                        )
//                    ).setTop(true)
//                        .setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP) as ContactItemBean
//                )
//                dataSource.add(
//                    ContactItemBean(
//                        TUIContactService.getAppContext().resources.getString(
//                            R.string.blacklist
//                        )
//                    ).setTop(true)
//                        .setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP) as ContactItemBean
//                )
                provider.loadFriendListDataAsync(callback)
            }
            else -> {}
        }
    }

    val nextSeq: Long
        get() = provider.nextSeq

    fun loadGroupMemberList(groupId: String) {
        if (!isSelectForCall && nextSeq == 0L) {
            dataSource.add(
                ContactItemBean(TUIContactService.getAppContext().resources.getString(R.string.at_all))
                    .setTop(true)
                    .setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP) as ContactItemBean
            )
        }
        provider.loadGroupMembers(groupId, object : IUIKitCallback<List<ContactItemBean>> {
            override fun onSuccess(data: List<ContactItemBean>) {
                TUIContactLog.i(
                    TAG,
                    "load data source success , loadType = " + ContactListView.DataSource.GROUP_MEMBER_LIST
                )
                onDataLoaded(data)
            }

            override fun onError(module: String, errCode: Int, errMsg: String) {
                TUIContactLog.e(
                    TAG,
                    "load data source error , loadType = " + ContactListView.DataSource.GROUP_MEMBER_LIST +
                            "  " + "errCode = " + errCode + "  errMsg = " + errMsg
                )
                onDataLoaded(ArrayList())
            }
        })
    }

    private fun onDataLoaded(loadedData: List<ContactItemBean>) {
        dataSource.addAll(loadedData)
        Log.e(TAG, "dataSource size: ${dataSource.size}")
        notifyDataSourceChanged()
    }

    private fun notifyDataSourceChanged() {
        contactListView?.onDataSourceChanged(dataSource)
    }

    private fun onDataListDeleted(userList: List<String>) {
        val userIterator = dataSource.iterator()
        while (userIterator.hasNext()) {
            val contactItemBean = userIterator.next()
            for (id in userList) {
                if (TextUtils.equals(id, contactItemBean.id)) {
                    userIterator.remove()
                }
            }
        }
        notifyDataSourceChanged()
    }

    private fun onDataListAdd(users: List<ContactItemBean>) {
        val addUserList: MutableList<ContactItemBean> = ArrayList(users)
        val beanIterator = addUserList.iterator()
        while (beanIterator.hasNext()) {
            val contactItemBean = beanIterator.next()
            for (dataItemBean in dataSource) {
                if (TextUtils.equals(contactItemBean.id, dataItemBean.id)) {
                    beanIterator.remove()
                }
            }
        }
        dataSource.addAll(addUserList)
        notifyDataSourceChanged()
    }

    fun getFriendApplicationUnreadCount(callback: IUIKitCallback<Int>) {
        provider.getFriendApplicationListUnreadCount(callback)
    }

    fun loadFriendApplicationList(callback: IUIKitCallback<Int>?) {
        provider.loadFriendApplicationList(object : IUIKitCallback<List<FriendApplicationBean>> {
            override fun onSuccess(data: List<FriendApplicationBean>) {
                var size = 0
                for (friendApplicationBean in data) {
                    if (friendApplicationBean.addType == FriendApplicationBean.FRIEND_APPLICATION_COME_IN) {
                        size++
                    }
                }
                ContactUtils.callbackOnSuccess(callback, size)
            }

            override fun onError(module: String, errCode: Int, errMsg: String) {
                ContactUtils.callbackOnError(callback, module, errCode, errMsg)
            }
        })
    }

    fun createGroupChat(groupInfo: GroupInfo, callback: IUIKitCallback<String>?) {
        provider.createGroupChat(groupInfo, object : IUIKitCallback<String> {
            override fun onSuccess(groupId: String) {
                groupInfo.id = groupId
                val gson = Gson()
                val messageCustom = MessageCustom()
                messageCustom.version = TUIContactConstants.version
                messageCustom.businessID = MessageCustom.BUSINESS_ID_GROUP_CREATE
                messageCustom.opUser = TUILogin.getLoginUser()
                messageCustom.content =
                    TUIContactService.getAppContext().getString(R.string.create_group)
                val data = gson.toJson(messageCustom)
                ThreadHelper.INST.execute {
                    try {
                        Thread.sleep(200)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    sendGroupTipsMessage(groupId, data, object : IUIKitCallback<String> {
                        override fun onSuccess(result: String) {
                            BackgroundTasks.getInstance()
                                .runOnUiThread { ContactUtils.callbackOnSuccess(callback, result) }
                        }

                        override fun onError(module: String, errCode: Int, errMsg: String) {
                            BackgroundTasks.getInstance().runOnUiThread {
                                ContactUtils.callbackOnError(callback, module, errCode, errMsg)
                            }
                        }
                    })
                }
            }

            override fun onError(module: String, errCode: Int, errMsg: String) {
                ContactUtils.callbackOnError(callback, module, errCode, errMsg)
            }
        })
    }

    fun onFriendInfoChanged(infoList: List<ContactItemBean>) {
        for (changedItem in infoList) {
            for (i in dataSource.indices) {
                if (TextUtils.equals(dataSource[i].id, changedItem.id)) {
                    dataSource[i] = changedItem
                    break
                }
            }
        }
        notifyDataSourceChanged()
    }

    fun onFriendRemarkChanged(id: String, remark: String) {
        loadDataSource(MyContactListView.DataSource.CONTACT_LIST)
    }

    fun sendGroupTipsMessage(
        groupId: String,
        messageData: String,
        callback: IUIKitCallback<String>
    ) {
        provider.sendGroupTipsMessage(groupId, messageData, callback)
    }

    companion object {
        private val TAG = "MCntactPrer"
    }

}