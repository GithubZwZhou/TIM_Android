package com.tencent.qcloud.tim.demo.thirdpush.TPNSPush

import android.content.Context
import android.text.TextUtils
import com.tencent.android.tpush.XGIOperateCallback
import com.tencent.android.tpush.XGPushConfig
import com.tencent.android.tpush.XGPushManager
import com.tencent.android.tpush.XGPushManager.AccountInfo
import com.tencent.qcloud.tim.demo.DemoApplication
import com.tencent.qcloud.tim.demo.thirdpush.PushSettingInterface
import com.tencent.qcloud.tim.demo.thirdpush.ThirdPushTokenMgr
import com.tencent.qcloud.tim.demo.utils.DemoLog
import com.tencent.qcloud.tim.demo.utils.PrivateConstants
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.tpns.baseapi.XGApiConfig


class TPNSPushSetting : PushSettingInterface {
    override fun init() {
        // 关闭 TPNS SDK 拉活其他 app 的功能
        // ref: https://cloud.tencent.com/document/product/548/36674#.E5.A6.82.E4.BD.95.E5.85.B3.E9.97.AD-tpns-.E7.9A.84.E4.BF.9D.E6.B4.BB.E5.8A.9F.E8.83.BD.EF.BC.9F
        XGPushConfig.enablePullUpOtherApp(DemoApplication.mApplication!!, false)

        // tpns sdk一个bug，后续版本可以去掉
        // setAccessId 可能会触发了当前版本一个已知bug，调用 setAccessId 且调用时机靠前的话，会重置TPNS SDK内缓存，导致TPNS token 刷新。当前版本可以通过set前先get 一次来规避。
        XGPushConfig.getAccessId(DemoApplication.mApplication!!)

        // 设置接入点, 注意：集群的切换将需要下次启动app生效。
        XGApiConfig.setServerSuffix(DemoApplication.mApplication!!, PrivateConstants.TPNS_SERVER_SUFFIX)
        XGPushConfig.setAccessId(DemoApplication.mApplication!!, PrivateConstants.TPNS_ACCESS_ID)
        XGPushConfig.setAccessKey(DemoApplication.mApplication!!, PrivateConstants.TPNS_ACCESS_KEY)

        // TPNS SDK 注册
        prepareTPNSRegister()
    }

    override fun bindUserID(userId: String) {
        // 重要：IM 登录用户账号时，调用 TPNS 账号绑定接口绑定业务账号，即可以此账号为目标进行 TPNS 离线推送
        val accountInfo = AccountInfo(
            XGPushManager.AccountType.UNKNOWN.value, userId
        )
        XGPushManager.upsertAccounts(
            DemoApplication.mApplication!!,
            listOf(accountInfo),
            object : XGIOperateCallback {
                override fun onSuccess(o: Any, i: Int) {
                    DemoLog.w(TAG, "upsertAccounts success")
                }

                override fun onFail(o: Any, i: Int, s: String) {
                    DemoLog.w(TAG, "upsertAccounts failed")
                }
            })
    }

    override fun unBindUserID(userId: String) {
        DemoLog.d(TAG, "tpns 解绑")
        // TPNS 账号解绑业务账号
        val xgiOperateCallback: XGIOperateCallback = object : XGIOperateCallback {
            override fun onSuccess(data: Any, flag: Int) {
                DemoLog.i(TAG, "onSuccess, data:$data, flag:$flag")
            }

            override fun onFail(data: Any, errCode: Int, msg: String) {
                DemoLog.w(TAG, "onFail, data:$data, code:$errCode, msg:$msg")
            }
        }

        //XGPushManager.delAccount(context, UserInfo.getInstance().getUserId(), xgiOperateCallback);
        val accountTypeSet: MutableSet<Int> = HashSet()
        accountTypeSet.add(XGPushManager.AccountType.CUSTOM.value)
        accountTypeSet.add(XGPushManager.AccountType.IMEI.value)
        XGPushManager.delAccounts(DemoApplication.mApplication!!, accountTypeSet, xgiOperateCallback)
    }

    override fun unInit() {
        DemoLog.d(TAG, "tpns 反注册")
        XGPushManager.unregisterPush(DemoApplication.mApplication!!, object : XGIOperateCallback {
            override fun onSuccess(data: Any, i: Int) {
                DemoLog.d(TAG, "反注册成功")
                ToastUtil.toastLongMessage("TPNS反注册成功")
            }

            override fun onFail(data: Any, errCode: Int, msg: String) {
                DemoLog.d(TAG, "反注册失败，错误码：$errCode,错误信息：$msg")
            }
        })
    }

    /**
     * TPNS SDK 推送服务注册接口
     *
     * 小米、魅族、OPPO 的厂商通道配置通过接口设置，
     * 华为、vivo 的厂商通道配置需在 AndroidManifest.xml 文件内添加，
     * FCM 通过 FCM 配置文件。
     */
    private fun prepareTPNSRegister() {
        DemoLog.i(TAG, "prepareTPNSRegister()")
        val context: Context = DemoApplication.mApplication!!
        XGPushConfig.enableDebug(context, true)
        XGPushConfig.setMiPushAppId(context, PrivateConstants.XM_PUSH_APPID)
        XGPushConfig.setMiPushAppKey(context, PrivateConstants.XM_PUSH_APPKEY)
        XGPushConfig.setMzPushAppId(context, PrivateConstants.MZ_PUSH_APPID)
        XGPushConfig.setMzPushAppKey(context, PrivateConstants.MZ_PUSH_APPKEY)
        XGPushConfig.setOppoPushAppId(context, PrivateConstants.OPPO_PUSH_APPKEY)
        XGPushConfig.setOppoPushAppKey(context, PrivateConstants.OPPO_PUSH_APPSECRET)

        // 重要：开启厂商通道注册
        XGPushConfig.enableOtherPush(context, true)

        // 注册 TPNS 推送服务
        XGPushManager.registerPush(context, object : XGIOperateCallback {
            override fun onSuccess(o: Any, i: Int) {
                DemoLog.w(TAG, "tpush register success token: $o")
                val token = o as String
                if (!TextUtils.isEmpty(token)) {
                    ThirdPushTokenMgr.getInstance().thirdPushToken = token
                    ThirdPushTokenMgr.getInstance().setPushTokenToTIM()
                }

                // 重要：获取通过 TPNS SDK 注册到的厂商推送 token，并调用 IM 接口设置和上传。
                if (XGPushConfig.isUsedOtherPush(context)) {
                    val otherPushToken = XGPushConfig.getOtherPushToken(context)
                    DemoLog.w(TAG, "otherPushToken token: $otherPushToken")
                }
            }

            override fun onFail(o: Any, i: Int, s: String) {
                DemoLog.w(TAG, "tpush register failed errCode: $i, errMsg: $s")
            }
        })
    }

    companion object {
        private val TAG = TPNSPushSetting::class.java.simpleName
    }
}