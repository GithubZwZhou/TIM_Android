package com.tencent.qcloud.tim.demo.contact

import com.tencent.qcloud.tuicore.component.interfaces.ILayout

interface MyIContactLayout : ILayout {
    fun getContactListView(): MyContactListView
}