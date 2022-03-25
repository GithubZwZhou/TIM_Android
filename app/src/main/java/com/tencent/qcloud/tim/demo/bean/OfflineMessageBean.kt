package com.tencent.qcloud.tim.demo.bean

import com.tencent.imsdk.v2.V2TIMConversation

class OfflineMessageBean {
    @kotlin.jvm.JvmField
    var version = 1
    @kotlin.jvm.JvmField
    var chatType = V2TIMConversation.V2TIM_C2C
    @kotlin.jvm.JvmField
    var action = REDIRECT_ACTION_CHAT
    @kotlin.jvm.JvmField
    var sender = ""
    @kotlin.jvm.JvmField
    var nickname = ""
    var faceUrl = ""
    @kotlin.jvm.JvmField
    var content = ""

    // 发送时间戳，单位秒
    @kotlin.jvm.JvmField
    var sendTime: Long = 0
    override fun toString(): String {
        return "OfflineMessageBean{" +
                "version=" + version +
                ", chatType='" + chatType + '\'' +
                ", action=" + action +
                ", sender=" + sender +
                ", nickname=" + nickname +
                ", faceUrl=" + faceUrl +
                ", content=" + content +
                ", sendTime=" + sendTime +
                '}'
    }

    companion object {
        const val REDIRECT_ACTION_CHAT = 1
        const val REDIRECT_ACTION_CALL = 2
    }
}