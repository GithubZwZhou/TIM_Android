package com.tencent.qcloud.tim.demo.thirdpush.OEMPush

import android.content.Context
import com.vivo.push.sdk.OpenClientPushMessageReceiver
import com.vivo.push.model.UPSNotificationMessage
import com.tencent.qcloud.tim.demo.utils.DemoLog

class VIVOPushMessageReceiverImpl : OpenClientPushMessageReceiver() {
    override fun onNotificationMessageClicked(
        context: Context,
        upsNotificationMessage: UPSNotificationMessage
    ) {
        // 点击vivo的通知栏时，会有两个动作，一个是触发该回调，另一个是会根据vivo的跳转配置启动activity
        // 所以这里需要把自定义的数据写到静态缓存里，启动配置activity的时候获取，进而进行下一步动作。
        //
        // 需要注意的是，如果这里携带解析的自定义数据来启动activity时，因为点击通知栏启动了相同的activity，
        // 等于启动了两遍同样的activity，因为时序无法保证，所以可能携带自定义数据的activity晚启动，跳转到聊天窗口失败
        DemoLog.i(
            TAG,
            "onNotificationMessageClicked upsNotificationMessage $upsNotificationMessage"
        )
        val extra = upsNotificationMessage.params
        sExt = extra["ext"]
    }

    override fun onReceiveRegId(context: Context, regId: String) {
        // vivo regId有变化会走这个回调。根据官网文档，获取regId需要在开启推送的回调里面调用PushClient.getInstance(getApplicationContext()).getRegId();参考LoginActivity
        DemoLog.i(TAG, "onReceiveRegId = $regId")
    }

    companion object {
        private val TAG = VIVOPushMessageReceiverImpl::class.java.simpleName
        private var sExt: String? = ""

        // 确保获取一次之后就释放，不会造成数据被滥用
        val params: String?
            get() {
                // 确保获取一次之后就释放，不会造成数据被滥用
                val tmp = sExt
                sExt = ""
                return tmp
            }
    }
}