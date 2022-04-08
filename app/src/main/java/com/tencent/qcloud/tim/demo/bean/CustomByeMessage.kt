package com.tencent.qcloud.tim.demo.bean

import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tuikit.tuichat.TUIChatConstants
import com.tencent.qcloud.tuikit.tuichat.TUIChatService

/**
 * 自定义消息的bean实体，用来与json的相互转化
 */
class CustomByeMessage {
    var businessID = TUIChatConstants.BUSINESS_ID_CUSTOM_HELLO
    var text = TUIChatService.getAppContext().getString(R.string.end_tip)
    var link = "https://cloud.tencent.com/document/product/269/3794"
    var version = TUIChatConstants.JSON_VERSION_UNKNOWN

    companion object {
        const val CUSTOM_HELLO_ACTION_ID = 3
    }
}