package com.tencent.qcloud.tim.demo.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMUserFullInfo
import com.tencent.imsdk.v2.V2TIMValueCallback
import com.tencent.qcloud.tim.demo.contact.ContactItemBeanWrapper
import com.tencent.qcloud.tuicore.util.ErrorMessageConverter
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactItemBean
import com.tencent.qcloud.tuikit.tuicontact.model.ContactProvider
import com.tencent.qcloud.tuikit.tuicontact.util.ContactUtils
import com.tencent.qcloud.tuikit.tuicontact.util.TUIContactLog

class ChatFinishViewModel : ViewModel() {
    val message = MutableLiveData<String>()
    private val _contractBean = MutableLiveData<ContactItemBeanWrapper>()
    val contactItemBean: LiveData<ContactItemBeanWrapper> get() = _contractBean

    // FriendProfileLayout.loadUserProfile
    fun getUserContactWrapperInfo(beanId: String) {
        TUIContactLog.e(TAG, "loadUserProfile...")
        V2TIMManager.getInstance()
            .getUsersInfo(listOf(beanId), object : V2TIMValueCallback<List<V2TIMUserFullInfo>> {
                override fun onError(code: Int, desc: String) {
                    TUIContactLog.e(
                        TAG,
                        "loadUserProfile err code = $code, desc = "
                                + ErrorMessageConverter.convertIMError(code, desc)
                    )
                    message.postValue("Error Code: $code, $desc")
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
                    val contactItemBeanWrapper = ContactItemBeanWrapper(contactItemBeanList[0])
                    TUIContactLog.e(
                        TAG,
                        "loadUserProfile: $contactItemBeanWrapper "
                    )
                    // TODO 添加二次接口
                    _contractBean.postValue(contactItemBeanWrapper)
                }
            })
    }

    private companion object {
        const val TAG = "ChatFinishVM"
    }
}

@Suppress("unchecked_cast")
class ChatFinishViewModelProvider : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatFinishViewModel() as T
    }

}