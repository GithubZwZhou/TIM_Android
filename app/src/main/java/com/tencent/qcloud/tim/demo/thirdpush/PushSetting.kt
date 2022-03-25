package com.tencent.qcloud.tim.demo.thirdpush

import android.content.Context
import com.tencent.qcloud.tim.demo.thirdpush.PushSettingInterface
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.thirdpush.PushSetting
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.thirdpush.TPNSPush.TPNSPushSetting
import com.tencent.qcloud.tim.demo.thirdpush.OEMPush.OEMPushSetting

class PushSetting {
    companion object {
        /**
         * 是否接入 TPNS 标记
         *
         * @note demo 实现了厂商和 TPNS 两种方式，以此变量作为逻辑区分
         *
         * - 当接入推送方案选择 TPNS 通道，设置 isTPNSChannel 为 true，走 TPNS 推送逻辑；
         * - 当接入推送方案选择厂商通道，设置 isTPNSChannel 为 false，走厂商推送逻辑；
         */
        @JvmField
        var isTPNSChannel = false
    }

    fun initPush() {
        val pushSettingInterface: PushSettingInterface
        val sharedPreferences = DemoApplication.mApplication!!.getSharedPreferences("TUIKIT_DEMO_SETTINGS", Context.MODE_PRIVATE)
        isTPNSChannel = sharedPreferences.getBoolean("isTPNSChannel", true)
        DemoLog.i("PushSetting", "initPush isTPNSChannel = $isTPNSChannel")
        if (isTPNSChannel) {
            pushSettingInterface = TPNSPushSetting()
            pushSettingInterface.init()
        } else {
            pushSettingInterface = OEMPushSetting()
            pushSettingInterface.init()
        }
    }

    fun bindUserID(userId: String) {
        val pushSettingInterface: PushSettingInterface
        if (isTPNSChannel) {
            pushSettingInterface = TPNSPushSetting()
            pushSettingInterface.bindUserID(userId)
        }
    }

    fun unBindUserID(userId: String) {
        val pushSettingInterface: PushSettingInterface
        if (isTPNSChannel) {
            pushSettingInterface = TPNSPushSetting()
            pushSettingInterface.unBindUserID(userId)
        }
    }

    fun unInitPush() {
        val pushSettingInterface: PushSettingInterface
        if (isTPNSChannel) {
            pushSettingInterface = TPNSPushSetting()
            pushSettingInterface.unInit()
        }
    }
}