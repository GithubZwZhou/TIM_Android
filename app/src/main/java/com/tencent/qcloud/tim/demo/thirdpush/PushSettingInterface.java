package com.tencent.qcloud.tim.demo.thirdpush;

public interface PushSettingInterface {
    /**
     *  注册离线推送服务, IM 账号登录成功时调用
     */
    void init();
    /**
     *  TPNS 接口
     *
     *  IM 账号绑定 TPNS，可以此账号为目标进行 TPNS 离线推送，IM 账号登录成功时调用
     */
    void bindUserID(String userId);
    /**
     *  TPNS 接口
     *
     *  IM 账号解绑TPNS, 登出 IM 账号时调用该接口
     */
    void unBindUserID(String userId);
    /**
     *  TPNS 接口
     *
     *  反注册离线推送服务取消推送，TPNS 通道登出账号或者账号被踢下线时调用
     */
    void unInit();
}
