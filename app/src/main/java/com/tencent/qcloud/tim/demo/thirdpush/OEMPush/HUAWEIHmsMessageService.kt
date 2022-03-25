package com.tencent.qcloud.tim.demo.thirdpush.OEMPush

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.thirdpush.ThirdPushTokenMgr
import com.tencent.qcloud.tim.demo.utils.BrandUtil
import java.lang.Exception

class HUAWEIHmsMessageService : HmsMessageService() {
    override fun onMessageReceived(message: RemoteMessage) {
        DemoLog.i(TAG, "onMessageReceived message=$message")
    }

    override fun onMessageSent(msgId: String) {
        DemoLog.i(TAG, "onMessageSent msgId=$msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        DemoLog.i(TAG, "onSendError msgId=$msgId")
    }

    override fun onNewToken(token: String) {
        DemoLog.i(TAG, "onNewToken token=$token")
        ThirdPushTokenMgr.getInstance().thirdPushToken = token
        ThirdPushTokenMgr.getInstance().setPushTokenToTIM()
    }

    override fun onTokenError(exception: Exception) {
        DemoLog.i(TAG, "onTokenError exception=$exception")
    }

    override fun onMessageDelivered(msgId: String, exception: Exception) {
        DemoLog.i(TAG, "onMessageDelivered msgId=$msgId")
    }

    companion object {
        private val TAG = HUAWEIHmsMessageService::class.java.simpleName
        fun updateBadge(context: Context, number: Int) {
            if (!BrandUtil.isBrandHuawei()) {
                return
            }
            DemoLog.i(TAG, "huawei badge = $number")
            try {
                val extra = Bundle()
                extra.putString("package", "com.tencent.qcloud.tim.tuikit")
                extra.putString("class", "com.tencent.qcloud.tim.demo.SplashActivity")
                extra.putInt("badgenumber", number)
                context.contentResolver.call(
                    Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                    "change_badge",
                    null,
                    extra
                )
            } catch (e: Exception) {
                DemoLog.w(TAG, "huawei badge exception: " + e.localizedMessage)
            }
        }
    }
}