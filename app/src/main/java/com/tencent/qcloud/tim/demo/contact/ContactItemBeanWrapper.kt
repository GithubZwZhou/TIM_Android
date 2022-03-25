package com.tencent.qcloud.tim.demo.contact

import android.util.Log
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactItemBean
import com.tencent.qcloud.tuikit.tuicontact.component.indexlib.indexbar.bean.BaseIndexPinyinBean

data class ContactItemBeanWrapper(
    val bean: ContactItemBean
) : BaseIndexPinyinBean() {

    override fun getTarget(): String {
        return bean.target
    }

    override fun isNeedToPinyin(): Boolean {
        return bean.isNeedToPinyin
    }

    override fun isShowSuspension(): Boolean {
        return bean.isShowSuspension
    }
}

fun ContactItemBean.convertToBeanWrapper() = ContactItemBeanWrapper(this)

fun List<ContactItemBean>.convertToWrapperList(): List<ContactItemBeanWrapper> {
    val list = ArrayList<ContactItemBeanWrapper>(this.size)
    Log.e("ContractItemBeanWrapper", "data size: ${this.size}")
    this.forEach {
        list.add(it.convertToBeanWrapper())
    }
    return list
}