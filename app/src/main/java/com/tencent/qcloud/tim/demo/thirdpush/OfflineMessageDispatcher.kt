package com.tencent.qcloud.tim.demo.thirdpush

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import com.google.gson.Gson
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMSignalingInfo
import com.tencent.qcloud.tim.demo.DemoApplication.Companion.mApplication
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.bean.CallModel
import com.tencent.qcloud.tim.demo.bean.OfflineMessageBean
import com.tencent.qcloud.tim.demo.bean.OfflineMessageContainerBean
import com.tencent.qcloud.tim.demo.main.MainActivity
import com.tencent.qcloud.tim.demo.thirdpush.OEMPush.VIVOPushMessageReceiverImpl
import com.tencent.qcloud.tim.demo.utils.BrandUtil
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.TUIUtils
import com.tencent.qcloud.tuicore.util.ErrorMessageConverter
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageHelper

object OfflineMessageDispatcher {
    private val TAG = OfflineMessageDispatcher::class.java.simpleName
    private const val OEMMessageKey = "ext"
    private const val TPNSMessageKey = "customContent"

    /**
     * 从 [intent] 中获取 [OfflineMessageBean]
     */
    fun parseOfflineMessage(intent: Intent): OfflineMessageBean? {
        DemoLog.i(TAG, "intent: $intent")
        return if (PushSetting.isTPNSChannel) {
            // TPNS 的透传参数数据格式为 customContent:{data}
            val uri = intent.data
            if (uri == null) {
                DemoLog.i(TAG, "intent.getData() uri is null")
                // intent 方式解析拿不到数据，试试 bundle 方式
                parseOfflineMessageTPNS2(intent)
            } else {
                parseOfflineMessageTPNS(intent)
            }
        } else {
            // OEM 厂商的透传参数数据格式为 ext:{data}
            parseOfflineMessageOEM(intent)
        }
    }

    /**
     * 通过 [Intent.getData] 获取文本并调用 [getOfflineMessageBeanFromContainer]
     * 获取 TPNS 方式下的 [OfflineMessageBean]。
     * TPNS 的透传参数数据格式为 customContent:{data}
     * 如果没有消息的话，返回 NULL。
     */
    private fun parseOfflineMessageTPNS(intent: Intent): OfflineMessageBean? {
        DemoLog.i(TAG, "parse TPNS push")
        //intent uri
        val uri = intent.data
        if (uri == null) {
            DemoLog.i(TAG, "intent.getData() uri is null")
        } else {
            DemoLog.i(TAG, "parseOfflineMessageTPNS get data uri: $uri")
            val ext = uri.getQueryParameter(TPNSMessageKey)
            DemoLog.i(TAG, "push custom data ext: $ext")
            if (!TextUtils.isEmpty(ext)) {
                return getOfflineMessageBeanFromContainer(ext!!)
            } else {
                DemoLog.i(TAG, "TextUtils.isEmpty(ext)")
            }
        }
        return null
    }

    /**
     * 通过 [Intent.getExtras] 获取 [Bundle] 并调用 [getOfflineMessageBeanFromContainer]
     * 获取 TPNS 方式下的 OfflineMessageBean。
     * 需要根据不同的品牌调用不同的获取方法。
     * 如果没有消息的话，返回 NULL。
     */
    private fun parseOfflineMessageTPNS2(intent: Intent): OfflineMessageBean? {
        DemoLog.i(TAG, "parse TPNS2 push")
        val bundle = intent.extras
        DemoLog.i(TAG, "bundle: $bundle")
        return if (bundle == null) {
            val ext = VIVOPushMessageReceiverImpl.params
            if (!TextUtils.isEmpty(ext)) {
                getOfflineMessageBeanFromContainer(ext!!)
            } else null
        } else {
            var ext = bundle.getString(TPNSMessageKey)
            DemoLog.i(TAG, "push custom data ext: $ext")
            if (TextUtils.isEmpty(ext)) {
                if (BrandUtil.isBrandXiaoMi()) {
                    ext = getXiaomiMessage(bundle)
                    return getOfflineMessageBeanFromContainer(ext)
                } else if (BrandUtil.isBrandOppo()) {
                    ext = getOPPOMessage(bundle)
                    return getOfflineMessageBean(ext)
                }
            } else {
                return getOfflineMessageBeanFromContainer(ext!!)
            }
            null
        }
    }

    /**
     * 通过 [Intent.getExtras] 获取文本并调用 [getOfflineMessageBeanFromContainer]
     * 或 [getOfflineMessageBean] 获取 OEM 方式下的 [OfflineMessageBean]。
     * OEM 厂商的透传参数数据格式为 ext:{data}.
     * 如果没有消息的话，返回 NULL。
     */
    private fun parseOfflineMessageOEM(intent: Intent): OfflineMessageBean? {
        DemoLog.i(TAG, "parse OEM push")
        val bundle = intent.extras
        DemoLog.i(TAG, "bundle: $bundle")
        return if (bundle == null) {
            val ext = VIVOPushMessageReceiverImpl.params
            if (!TextUtils.isEmpty(ext)) {
                getOfflineMessageBeanFromContainer(ext!!)
            } else null
        } else {
            var ext = bundle.getString(OEMMessageKey)
            DemoLog.i(TAG, "push custom data ext: $ext")
            if (TextUtils.isEmpty(ext)) {
                if (BrandUtil.isBrandXiaoMi()) {
                    ext = getXiaomiMessage(bundle)
                    return getOfflineMessageBeanFromContainer(ext)
                } else if (BrandUtil.isBrandOppo()) {
                    ext = getOPPOMessage(bundle)
                    return getOfflineMessageBean(ext)
                }
            } else {
                return getOfflineMessageBeanFromContainer(ext!!)
            }
            null
        }
    }

    private fun getXiaomiMessage(bundle: Bundle): String {
        val miPushMessage = bundle.getSerializable(PushMessageHelper.KEY_MESSAGE) as MiPushMessage?
        return miPushMessage?.extra?.get(OEMMessageKey) ?: ""
    }

    private fun getOPPOMessage(bundle: Bundle): String {
        val set = bundle.keySet()
        if (set != null) {
            for (key in set) {
                val value = bundle[key]
                DemoLog.i(TAG, "push custom data key: $key value: $value")
                if (TextUtils.equals("entity", key)) {
                    return value.toString()
                }
            }
        }
        return ""
    }

    /**
     * 根据 [ext] 通过 Gson 解析出 [OfflineMessageContainerBean]
     * 并进行验证 [offlineMessageBeanValidCheck]。
     * 没有消息 / 异常 / 验证失败 均返回 NULL。
     */
    private fun getOfflineMessageBeanFromContainer(ext: String): OfflineMessageBean? {
        if (TextUtils.isEmpty(ext)) {
            return null
        }
        var bean: OfflineMessageContainerBean? = null
        try {
            bean = Gson().fromJson(ext, OfflineMessageContainerBean::class.java)
        } catch (e: Exception) {
            DemoLog.w(TAG, "getOfflineMessageBeanFromContainer: " + e.message)
        }
        return if (bean != null) {
            offlineMessageBeanValidCheck(bean.entity)
        } else {
            null
        }
    }

    /**
     * 根据 [ext] 通过 Gson 解析出 [OfflineMessageBean]
     * 并进行验证 [offlineMessageBeanValidCheck]。
     * 没有消息 / 异常 / 验证失败 均返回 NULL。
     */
    private fun getOfflineMessageBean(ext: String): OfflineMessageBean? {
        if (TextUtils.isEmpty(ext)) {
            return null
        }
        val bean = Gson().fromJson(ext, OfflineMessageBean::class.java)
        return offlineMessageBeanValidCheck(bean)
    }

    /**
     * 对离线消息[bean] 进行验证，返回 [OfflineMessageBean] 或 NULL。
     */
    private fun offlineMessageBeanValidCheck(bean: OfflineMessageBean): OfflineMessageBean? {
        if (bean.version != 1
            || (bean.action != OfflineMessageBean.REDIRECT_ACTION_CHAT
                    && bean.action != OfflineMessageBean.REDIRECT_ACTION_CALL)
        ) {
            val packageManager = mApplication!!.packageManager
            val label =
                packageManager.getApplicationLabel(mApplication!!.applicationInfo).toString()
            ToastUtil.toastLongMessage(
                mApplication!!.getString(R.string.you_app) + label + mApplication!!.getString(
                    R.string.low_version
                )
            )
            DemoLog.e(TAG, "unknown version: " + bean.version + " or action: " + bean.action)
            return null
        }
        return bean
    }

    /**
     * 根据 [bean] 对消息进行 重定向 [startChat] 或 [redirectCall]
     * 处理成功后 返回 true
     */
    fun redirect(bean: OfflineMessageBean): Boolean {
        if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CHAT) {
            if (TextUtils.isEmpty(bean.sender)) {
                return true
            }
            TUIUtils.startChat(bean.sender, bean.nickname, bean.chatType)
            return true
        } else if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CALL) {
            redirectCall(bean)
        }
        return true
    }

    /**
     * 解析 [bean] 到 [CallModel] 重定向到 [MainActivity]，
     * 如果是群组通话，则在 [V2TIMManager] 通过后回调 [TUIUtils.startCall]
     */
    private fun redirectCall(bean: OfflineMessageBean) {
        val model = Gson().fromJson(bean.content, CallModel::class.java) ?: return
        DemoLog.i(TAG, "bean: $bean model: $model")
        model.sender = bean.sender
        model.data = bean.content
        val timeout = V2TIMManager.getInstance().serverTime - bean.sendTime
        // 处理 通话超时
        if (timeout >= model.timeout) {
            ToastUtil.toastLongMessage(mApplication!!.getString(R.string.call_time_out))
            return
        }
        // 跳转到 MainActivity，并开启通话
        if (TextUtils.isEmpty(model.groupId)) {
            val mainIntent = Intent(mApplication, MainActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mApplication!!.startActivity(mainIntent)
        } else {
            val info = V2TIMSignalingInfo()
            info.inviteID = model.callId
            info.inviteeList = model.invitedList
            info.groupID = model.groupId
            info.inviter = bean.sender
            V2TIMManager.getSignalingManager()
                .addInvitedSignaling(info, object : V2TIMCallback {
                    override fun onError(code: Int, desc: String) {
                        DemoLog.e(
                            TAG,
                            "addInvitedSignaling code: $code desc: " + ErrorMessageConverter.convertIMError(
                                code,
                                desc
                            )
                        )
                    }

                    override fun onSuccess() {
                        val mainIntent = Intent(mApplication, MainActivity::class.java)
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        mApplication!!.startActivity(mainIntent)
                        TUIUtils.startCall(bean.sender, model.data)
                    }
                })
        }
    }

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