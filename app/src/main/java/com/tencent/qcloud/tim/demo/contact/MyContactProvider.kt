package com.tencent.qcloud.tim.demo.contact

import android.text.TextUtils
import android.util.Pair
import com.tencent.imsdk.v2.*
import com.tencent.qcloud.tuicore.component.interfaces.IUIKitCallback
import com.tencent.qcloud.tuicore.util.ErrorMessageConverter
import com.tencent.qcloud.tuicore.util.ThreadHelper
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactGroupApplyInfo
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactItemBean
import com.tencent.qcloud.tuikit.tuicontact.bean.FriendApplicationBean
import com.tencent.qcloud.tuikit.tuicontact.bean.GroupInfo
import com.tencent.qcloud.tuikit.tuicontact.util.ContactUtils
import com.tencent.qcloud.tuikit.tuicontact.util.TUIContactLog


class MyContactProvider {
    var nextSeq: Long = 0

    // TODO 需要修改这里的逻辑以达到 在线及好友的功能
    fun loadFriendListDataAsync(callback: IUIKitCallback<List<ContactItemBean>>?) {
        TUIContactLog.i(TAG, "loadFriendListDataAsync")
        ThreadHelper.INST.execute { // 压测时数据量比较大，query耗时比较久，所以这里使用新线程来处理
            V2TIMManager.getFriendshipManager()
                .getFriendList(object : V2TIMValueCallback<List<V2TIMFriendInfo>> {
                    override fun onError(code: Int, desc: String) {
                        TUIContactLog.e(
                            TAG,
                            "loadFriendListDataAsync err code:$code, desc:"
                                    + ErrorMessageConverter.convertIMError(code, desc)
                        )
                        ContactUtils.callbackOnError(callback, TAG, code, desc)
                    }

                    override fun onSuccess(v2TIMFriendInfos: List<V2TIMFriendInfo>) {
                        val contactItemBeanList: MutableList<ContactItemBean> = ArrayList()
                        TUIContactLog.i(TAG, "loadFriendListDataAsync->getFriendList: ${v2TIMFriendInfos.size}")
                        for (timFriendInfo in v2TIMFriendInfos) {
                            val info = ContactItemBean()
                            info.isFriend = true
                            info.covertTIMFriend(timFriendInfo)
                            // TODO 在这里添加其他信息
                            contactItemBeanList.add(info)
                        }
                        ContactUtils.callbackOnSuccess(callback, contactItemBeanList)
                    }
                })
        }
    }

    fun loadBlackListData(callback: IUIKitCallback<List<ContactItemBean>>) {
        TUIContactLog.i(TAG, "loadBlackListData")
        V2TIMManager.getFriendshipManager()
            .getBlackList(object : V2TIMValueCallback<List<V2TIMFriendInfo>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "getBlackList err code = $code, desc = " + ErrorMessageConverter.convertIMError(
                            code,
                            desc
                        )
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendInfos: List<V2TIMFriendInfo>) {
                    TUIContactLog.i(TAG, "getBlackList success: " + v2TIMFriendInfos.size)
                    if (v2TIMFriendInfos.isEmpty()) {
                        TUIContactLog.i(TAG, "getBlackList success but no data")
                    }
                    val contactItemBeanList: MutableList<ContactItemBean> = ArrayList()
                    for (timFriendInfo in v2TIMFriendInfos) {
                        val info = ContactItemBean()
                        info.covertTIMFriend(timFriendInfo).isBlackList = true
                        contactItemBeanList.add(info)
                    }
                    ContactUtils.callbackOnSuccess(callback, contactItemBeanList)
                }
            })
    }

    fun loadGroupListData(callback: IUIKitCallback<List<ContactItemBean>>) {
        TUIContactLog.i(TAG, "loadGroupListData")
        V2TIMManager.getGroupManager()
            .getJoinedGroupList(object : V2TIMValueCallback<List<V2TIMGroupInfo>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "getGroupList err code = $code, desc = " + ErrorMessageConverter.convertIMError(
                            code,
                            desc
                        )
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMGroupInfos: List<V2TIMGroupInfo>) {
                    TUIContactLog.i(TAG, "getGroupList success: " + v2TIMGroupInfos.size)
                    if (v2TIMGroupInfos.isEmpty()) {
                        TUIContactLog.i(TAG, "getGroupList success but no data")
                    }
                    val contactItemBeanList: MutableList<ContactItemBean> = ArrayList()
                    for (info in v2TIMGroupInfos) {
                        val bean = ContactItemBean()
                        contactItemBeanList.add(bean.covertTIMGroupBaseInfo(info))
                    }
                    ContactUtils.callbackOnSuccess(callback, contactItemBeanList)
                }
            })
    }

    fun loadGroupMembers(groupId: String, callback: IUIKitCallback<List<ContactItemBean>>) {
        V2TIMManager.getGroupManager().getGroupMemberList(
            groupId,
            V2TIMGroupMemberFullInfo.V2TIM_GROUP_MEMBER_FILTER_ALL,
            nextSeq,
            object : V2TIMValueCallback<V2TIMGroupMemberInfoResult> {
                override fun onError(code: Int, desc: String) {
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                    TUIContactLog.e(
                        TAG,
                        "loadGroupMembers failed, code: $code|desc: " + ErrorMessageConverter.convertIMError(
                            code,
                            desc
                        )
                    )
                }

                override fun onSuccess(v2TIMGroupMemberInfoResult: V2TIMGroupMemberInfoResult) {
                    val members: MutableList<V2TIMGroupMemberFullInfo> = ArrayList()
                    for (i in v2TIMGroupMemberInfoResult.memberInfoList.indices) {
                        if (v2TIMGroupMemberInfoResult.memberInfoList[i].userID == V2TIMManager.getInstance().loginUser) {
                            continue
                        }
                        members.add(v2TIMGroupMemberInfoResult.memberInfoList[i])
                    }
                    nextSeq = v2TIMGroupMemberInfoResult.nextSeq
                    val contactItemBeanList: MutableList<ContactItemBean> = ArrayList()
                    for (info in members) {
                        val bean = ContactItemBean()
                        contactItemBeanList.add(bean.covertTIMGroupMemberFullInfo(info))
                    }
                    ContactUtils.callbackOnSuccess(callback, contactItemBeanList)
                }
            })
    }

    fun addFriend(
        userId: String,
        addWording: String,
        callback: IUIKitCallback<Pair<Int, String>>
    ) {
        addFriend(userId, addWording, null, null, callback)
    }

    private fun addFriend(
        userId: String,
        addWording: String,
        friendGroup: String?,
        remark: String?,
        callback: IUIKitCallback<Pair<Int, String>>
    ) {
        val v2TIMFriendAddApplication = V2TIMFriendAddApplication(userId)
        v2TIMFriendAddApplication.setAddWording(addWording)
        v2TIMFriendAddApplication.setAddSource("android")
        v2TIMFriendAddApplication.setFriendGroup(friendGroup)
        v2TIMFriendAddApplication.setFriendRemark(remark)
        V2TIMManager.getFriendshipManager().addFriend(
            v2TIMFriendAddApplication,
            object : V2TIMValueCallback<V2TIMFriendOperationResult> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "addFriend err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendOperationResult: V2TIMFriendOperationResult) {
                    TUIContactLog.i(TAG, "addFriend success")
                    ContactUtils.callbackOnSuccess(
                        callback,
                        Pair(
                            v2TIMFriendOperationResult.resultCode,
                            v2TIMFriendOperationResult.resultInfo
                        )
                    )
                }
            })
    }

    fun joinGroup(groupId: String, addWording: String, callback: IUIKitCallback<Void>) {
        V2TIMManager.getInstance().joinGroup(groupId, addWording, object : V2TIMCallback {
            override fun onError(code: Int, desc: String) {
                TUIContactLog.e(
                    TAG,
                    "addGroup err code = $code, desc = "
                            + ErrorMessageConverter.convertIMError(code, desc)
                )
                ContactUtils.callbackOnError(callback, TAG, code, desc)
            }

            override fun onSuccess() {
                TUIContactLog.i(TAG, "addGroup success")
                ContactUtils.callbackOnSuccess(callback, null)
            }
        })
    }

    fun loadFriendApplicationList(callback: IUIKitCallback<List<FriendApplicationBean>>) {
        V2TIMManager.getFriendshipManager()
            .getFriendApplicationList(object : V2TIMValueCallback<V2TIMFriendApplicationResult> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "getPendencyList err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendApplicationResult: V2TIMFriendApplicationResult) {
                    val applicationBeanList: MutableList<FriendApplicationBean> = ArrayList()
                    for (application in v2TIMFriendApplicationResult.friendApplicationList) {
                        val bean = FriendApplicationBean()
                        bean.convertFromTimFriendApplication(application)
                        applicationBeanList.add(bean)
                    }
                    ContactUtils.callbackOnSuccess(callback, applicationBeanList)
                }
            })
    }

    fun getFriendApplicationListUnreadCount(callback: IUIKitCallback<Int>) {
        V2TIMManager.getFriendshipManager()
            .getFriendApplicationList(object : V2TIMValueCallback<V2TIMFriendApplicationResult> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "getPendencyList err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendApplicationResult: V2TIMFriendApplicationResult) {
                    ContactUtils.callbackOnSuccess(
                        callback,
                        v2TIMFriendApplicationResult.unreadCount
                    )
                }
            })
    }

    private fun acceptFriendApplication(
        friendApplication: V2TIMFriendApplication,
        responseType: Int,
        callback: IUIKitCallback<Void>
    ) {
        V2TIMManager.getFriendshipManager().acceptFriendApplication(
            friendApplication,
            responseType,
            object : V2TIMValueCallback<V2TIMFriendOperationResult> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "acceptFriend err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                }

                override fun onSuccess(v2TIMFriendOperationResult: V2TIMFriendOperationResult) {
                    TUIContactLog.i(TAG, "acceptFriend success")
                    ContactUtils.callbackOnSuccess(callback, null)
                }
            })
    }

    fun acceptFriendApplication(
        bean: FriendApplicationBean,
        responseType: Int,
        callback: IUIKitCallback<Void>
    ) {
        val friendApplication = bean.friendApplication
        acceptFriendApplication(friendApplication, responseType, callback)
    }

    fun getC2CReceiveMessageOpt(userIdList: List<String>, callback: IUIKitCallback<Boolean>) {
        V2TIMManager.getMessageManager().getC2CReceiveMessageOpt(
            userIdList,
            object : V2TIMValueCallback<List<V2TIMReceiveMessageOptInfo>> {
                override fun onSuccess(V2TIMReceiveMessageOptInfos: List<V2TIMReceiveMessageOptInfo>) {
                    if (V2TIMReceiveMessageOptInfos.isEmpty()) {
                        TUIContactLog.d(TAG, "getC2CReceiveMessageOpt null")
                        ContactUtils.callbackOnError(
                            callback,
                            TAG,
                            -1,
                            "getC2CReceiveMessageOpt null"
                        )
                        return
                    }
                    val V2TIMReceiveMessageOptInfo = V2TIMReceiveMessageOptInfos[0]
                    val option = V2TIMReceiveMessageOptInfo.c2CReceiveMessageOpt
                    TUIContactLog.d(TAG, "getC2CReceiveMessageOpt option = $option")
                    ContactUtils.callbackOnSuccess(
                        callback,
                        option == V2TIMMessage.V2TIM_RECEIVE_NOT_NOTIFY_MESSAGE
                    )
                }

                override fun onError(code: Int, desc: String) {
                    TUIContactLog.d(
                        TAG,
                        "getC2CReceiveMessageOpt onError code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }
            })
    }

    fun setC2CReceiveMessageOpt(
        userIdList: List<String?>?,
        isReceiveMessage: Boolean,
        callback: IUIKitCallback<Void?>?
    ) {
        val option: Int = if (isReceiveMessage) {
            V2TIMMessage.V2TIM_RECEIVE_NOT_NOTIFY_MESSAGE
        } else {
            V2TIMMessage.V2TIM_RECEIVE_MESSAGE
        }
        V2TIMManager.getMessageManager()
            .setC2CReceiveMessageOpt(userIdList, option, object : V2TIMCallback {
                override fun onSuccess() {
                    TUIContactLog.d(TAG, "setC2CReceiveMessageOpt onSuccess")
                    ContactUtils.callbackOnSuccess(callback, null)
                }

                override fun onError(code: Int, desc: String) {
                    TUIContactLog.d(
                        TAG,
                        "setC2CReceiveMessageOpt onError code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }
            })
    }

    fun getGroupInfo(groupIds: List<String>, callback: IUIKitCallback<List<GroupInfo>>) {
        V2TIMManager.getGroupManager()
            .getGroupsInfo(groupIds, object : V2TIMValueCallback<List<V2TIMGroupInfoResult>> {
                override fun onSuccess(v2TIMGroupInfoResults: List<V2TIMGroupInfoResult>) {
                    val groupInfos: MutableList<GroupInfo> = ArrayList()
                    for (result in v2TIMGroupInfoResults) {
                        if (result.resultCode != 0) {
                            ContactUtils.callbackOnError(
                                callback,
                                result.resultCode,
                                result.resultMessage
                            )
                            return
                        }
                        val groupInfo = GroupInfo()
                        groupInfo.id = result.groupInfo.groupID
                        groupInfo.faceUrl = result.groupInfo.faceUrl
                        groupInfo.groupName = result.groupInfo.groupName
                        groupInfo.memberCount = result.groupInfo.memberCount
                        groupInfo.groupType = result.groupInfo.groupType
                        groupInfos.add(groupInfo)
                    }
                    ContactUtils.callbackOnSuccess(callback, groupInfos)
                }

                override fun onError(code: Int, desc: String) {
                    ContactUtils.callbackOnError(callback, code, desc)
                }
            })
    }

    fun getUserInfo(userIdList: List<String>, callback: IUIKitCallback<List<ContactItemBean>>) {
        V2TIMManager.getInstance()
            .getUsersInfo(userIdList, object : V2TIMValueCallback<List<V2TIMUserFullInfo>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "loadUserProfile err code = $code, desc = " + ErrorMessageConverter.convertIMError(
                            code,
                            desc
                        )
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMUserFullInfos: List<V2TIMUserFullInfo>) {
                    val contactItemBeanList: MutableList<ContactItemBean> = ArrayList()
                    for (userFullInfo in v2TIMUserFullInfos) {
                        val contactItemBean = ContactItemBean()
                        contactItemBean.nickName = userFullInfo.nickName
                        contactItemBean.id = userFullInfo.userID
                        contactItemBean.avatarUrl = userFullInfo.faceUrl
                        contactItemBean.signature = userFullInfo.selfSignature
                        contactItemBeanList.add(contactItemBean)
                    }
                    ContactUtils.callbackOnSuccess(callback, contactItemBeanList)
                }
            })
    }

    fun isInBlackList(id: String?, callback: IUIKitCallback<Boolean>) {
        V2TIMManager.getFriendshipManager()
            .getBlackList(object : V2TIMValueCallback<List<V2TIMFriendInfo>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "getBlackList err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendInfos: List<V2TIMFriendInfo>) {
                    if (v2TIMFriendInfos.isNotEmpty()) {
                        for (friendInfo in v2TIMFriendInfos) {
                            if (TextUtils.equals(friendInfo.userID, id)) {
                                ContactUtils.callbackOnSuccess(callback, true)
                                return
                            }
                        }
                    }
                    ContactUtils.callbackOnSuccess(callback, false)
                }
            })
    }

    fun isFriend(id: String, bean: ContactItemBean, callback: IUIKitCallback<Boolean>) {
        V2TIMManager.getFriendshipManager()
            .getFriendList(object : V2TIMValueCallback<List<V2TIMFriendInfo>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "getFriendList err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendInfos: List<V2TIMFriendInfo>) {
                    if (v2TIMFriendInfos.isNotEmpty()) {
                        for (friendInfo in v2TIMFriendInfos) {
                            if (TextUtils.equals(friendInfo.userID, id)) {
                                bean.isFriend = true
                                bean.remark = friendInfo.friendRemark
                                bean.avatarUrl = friendInfo.userProfile.faceUrl
                                ContactUtils.callbackOnSuccess(callback, true)
                                return
                            }
                        }
                    }
                    ContactUtils.callbackOnSuccess(callback, false)
                }
            })
    }

    fun deleteFromBlackList(idList: List<String>, callback: IUIKitCallback<Void>) {
        V2TIMManager.getFriendshipManager().deleteFromBlackList(
            idList,
            object : V2TIMValueCallback<List<V2TIMFriendOperationResult>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "deleteBlackList err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendOperationResults: List<V2TIMFriendOperationResult>) {
                    TUIContactLog.i(TAG, "deleteBlackList success")
                    ContactUtils.callbackOnSuccess(callback, null)
                }
            })
    }

    fun addToBlackList(idList: List<String>, callback: IUIKitCallback<Void>) {
        V2TIMManager.getFriendshipManager().addToBlackList(
            idList,
            object : V2TIMValueCallback<List<V2TIMFriendOperationResult>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "deleteBlackList err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendOperationResults: List<V2TIMFriendOperationResult>) {
                    TUIContactLog.i(TAG, "deleteBlackList success")
                    ContactUtils.callbackOnSuccess(callback, null)
                }
            })
    }

    fun modifyRemark(id: String, remark: String, callback: IUIKitCallback<String>) {
        val v2TIMFriendInfo = V2TIMFriendInfo()
        v2TIMFriendInfo.userID = id
        v2TIMFriendInfo.friendRemark = remark
        V2TIMManager.getFriendshipManager().setFriendInfo(v2TIMFriendInfo, object : V2TIMCallback {
            override fun onError(code: Int, desc: String) {
                TUIContactLog.e(
                    TAG,
                    "modifyRemark err code = $code, desc = "
                            + ErrorMessageConverter.convertIMError(code, desc)
                )
                ContactUtils.callbackOnError(callback, TAG, code, desc)
            }

            override fun onSuccess() {
                ContactUtils.callbackOnSuccess(callback, remark)
                TUIContactLog.i(TAG, "modifyRemark success")
            }
        })
    }

    fun deleteFriend(identifiers: List<String>, callback: IUIKitCallback<Void>) {
        V2TIMManager.getFriendshipManager().deleteFromFriendList(
            identifiers,
            V2TIMFriendInfo.V2TIM_FRIEND_TYPE_BOTH,
            object : V2TIMValueCallback<List<V2TIMFriendOperationResult>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "deleteFriends err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMFriendOperationResults: List<V2TIMFriendOperationResult>) {
                    TUIContactLog.i(TAG, "deleteFriends success")
                    ContactUtils.callbackOnSuccess(callback, null)
                }
            })
    }

    fun refuseFriendApplication(
        friendApplication: FriendApplicationBean,
        callback: IUIKitCallback<Void>
    ) {
        val v2TIMFriendApplication = friendApplication.friendApplication
        if (v2TIMFriendApplication == null) {
            ContactUtils.callbackOnError(
                callback,
                "refuseFriendApplication",
                -1,
                "V2TIMFriendApplication is null"
            )
            return
        }
        V2TIMManager.getFriendshipManager().refuseFriendApplication(
            v2TIMFriendApplication,
            object : V2TIMValueCallback<V2TIMFriendOperationResult> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "accept err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                    ToastUtil.toastShortMessage(
                        "Error code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                }

                override fun onSuccess(v2TIMFriendOperationResult: V2TIMFriendOperationResult) {
                    TUIContactLog.i(TAG, "refuse success")
                    ContactUtils.callbackOnSuccess(callback, null)
                }
            })
    }

    fun createGroupChat(groupInfo: GroupInfo, callback: IUIKitCallback<String>) {
        val v2TIMGroupInfo = V2TIMGroupInfo()
        v2TIMGroupInfo.groupType = groupInfo.groupType
        v2TIMGroupInfo.groupName = groupInfo.groupName
        v2TIMGroupInfo.groupAddOpt = groupInfo.joinType
        val v2TIMCreateGroupMemberInfoList: MutableList<V2TIMCreateGroupMemberInfo> = ArrayList()
        for (i in groupInfo.memberDetails.indices) {
            val groupMemberInfo = groupInfo.memberDetails[i]
            val v2TIMCreateGroupMemberInfo = V2TIMCreateGroupMemberInfo()
            v2TIMCreateGroupMemberInfo.setUserID(groupMemberInfo.account)
            v2TIMCreateGroupMemberInfoList.add(v2TIMCreateGroupMemberInfo)
        }
        V2TIMManager.getGroupManager().createGroup(
            v2TIMGroupInfo,
            v2TIMCreateGroupMemberInfoList,
            object : V2TIMValueCallback<String> {
                override fun onSuccess(s: String) {
                    ContactUtils.callbackOnSuccess(callback, s)
                }

                override fun onError(code: Int, desc: String) {
                    ContactUtils.callbackOnError(callback, code, desc)
                }
            })
    }

    fun sendGroupTipsMessage(groupId: String, message: String, callback: IUIKitCallback<String>) {
        val v2TIMMessage =
            V2TIMManager.getMessageManager().createCustomMessage(message.toByteArray())
        V2TIMManager.getMessageManager().sendMessage(v2TIMMessage,
            null,
            groupId,
            V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
            false,
            null,
            object : V2TIMSendCallback<V2TIMMessage> {
                override fun onProgress(progress: Int) {}
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.i(
                        TAG,
                        "sendTipsMessage error , code : $code desc : "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    ContactUtils.callbackOnError(callback, TAG, code, desc)
                }

                override fun onSuccess(v2TIMMessage: V2TIMMessage) {
                    TUIContactLog.i(TAG, "sendTipsMessage onSuccess")
                    ContactUtils.callbackOnSuccess(callback, groupId)
                }
            })
    }

    fun acceptJoinGroupApply(applyInfo: ContactGroupApplyInfo, callback: IUIKitCallback<Void>) {
        val application = applyInfo.timGroupApplication
        val reason = applyInfo.requestMsg
        V2TIMManager.getGroupManager()
            .acceptGroupApplication(application, reason, object : V2TIMCallback {
                override fun onSuccess() {
                    ContactUtils.callbackOnSuccess(callback, null)
                }

                override fun onError(code: Int, desc: String) {
                    ContactUtils.callbackOnError(callback, code, desc)
                }
            })
    }

    fun refuseJoinGroupApply(
        info: ContactGroupApplyInfo,
        reason: String,
        callback: IUIKitCallback<Void>
    ) {
        val application = info.timGroupApplication
        V2TIMManager.getGroupManager()
            .refuseGroupApplication(application, reason, object : V2TIMCallback {
                override fun onSuccess() {
                    ContactUtils.callbackOnSuccess(callback, null)
                }

                override fun onError(code: Int, desc: String) {
                    ContactUtils.callbackOnError(callback, code, desc)
                }
            })
    }

    fun setGroupApplicationRead(callback: IUIKitCallback<Void>) {
        V2TIMManager.getGroupManager().setGroupApplicationRead(object : V2TIMCallback {
            override fun onSuccess() {
                ContactUtils.callbackOnSuccess(callback, null)
            }

            override fun onError(code: Int, desc: String) {
                ContactUtils.callbackOnError(callback, code, desc)
            }
        })
    }

    companion object {
        private val TAG = "MyContactProvider"
    }
}