package com.tencent.qcloud.tim.demo.utils

import android.content.Context
import android.os.Bundle
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKListener
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.qcloud.tuicore.TUILogin
import java.util.HashMap

object TUIUtils {
    val TAG = TUIUtils::class.java.simpleName

    /**
     * @param context  应用的上下文，一般为对应应用的ApplicationContext
     * @param sdkAppID 您在腾讯云注册应用时分配的sdkAppID
     * @param config  IMSDK 的相关配置项，一般使用默认即可，需特殊配置参考API文档
     * @param listener  IMSDK 初始化监听器
     */
    fun init(
        context: Context,
        sdkAppID: Int,
        config: V2TIMSDKConfig?,
        listener: V2TIMSDKListener?
    ) {
        TUILogin.init(context, sdkAppID, config, listener)
    }

    /**
     * 释放一些资源等，一般可以在退出登录时调用
     */
    fun unInit() {
        TUILogin.unInit()
    }

    /**
     * 获取TUIKit保存的上下文Context，该Context会长期持有，所以应该为Application级别的上下文
     *
     * @return
     */
    val appContext: Context
        get() = TUILogin.getAppContext()

    /**
     * 用户IM登录
     *
     * @param userId   用户名
     * @param userSig  从业务服务器获取的userSig
     * @param callback 登录是否成功的回调
     */
    @JvmStatic
    fun login(userId: String, userSig: String, callback: V2TIMCallback?) {
        TUILogin.login(userId, userSig, object : V2TIMCallback {
            override fun onSuccess() {
                callback?.onSuccess()
            }

            override fun onError(code: Int, desc: String) {
                callback?.onError(code, desc)
            }
        })
    }

    @JvmStatic
    fun logout(callback: V2TIMCallback?) {
        TUILogin.logout(callback)
    }

    @JvmStatic
    fun startActivity(activityName: String?, param: Bundle?) {
        TUICore.startActivity(activityName, param)
    }

    @JvmStatic
    fun startChat(chatId: String?, chatName: String?, chatType: Int) {
        val bundle = Bundle()
        bundle.putString(TUIConstants.TUIChat.CHAT_ID, chatId)
        bundle.putString(TUIConstants.TUIChat.CHAT_NAME, chatName)
        bundle.putInt(TUIConstants.TUIChat.CHAT_TYPE, chatType)
        if (chatType == V2TIMConversation.V2TIM_C2C) {
            TUICore.startActivity(TUIConstants.TUIChat.C2C_CHAT_ACTIVITY_NAME, bundle)
        } else if (chatType == V2TIMConversation.V2TIM_GROUP) {
            TUICore.startActivity(TUIConstants.TUIChat.GROUP_CHAT_ACTIVITY_NAME, bundle)
        }
    }

    @JvmStatic
    fun startCall(sender: String, data: String) {
        val param: MutableMap<String, Any> = HashMap()
        param[TUIConstants.TUICalling.SENDER] = sender
        param[TUIConstants.TUICalling.PARAM_NAME_CALLMODEL] = data
        TUICore.callService(
            TUIConstants.TUICalling.SERVICE_NAME,
            TUIConstants.TUICalling.METHOD_START_CALL,
            param
        )
    }
}