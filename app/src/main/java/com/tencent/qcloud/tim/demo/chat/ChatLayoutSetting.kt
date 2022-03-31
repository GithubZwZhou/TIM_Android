package com.tencent.qcloud.tim.demo.chat

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.tencent.qcloud.tuikit.tuichat.ui.view.ChatView
import com.tencent.qcloud.tuikit.tuichat.ui.view.message.MessageRecyclerView

class ChatLayoutSetting {
    private var groupId: String? = null
    private var conversationDuring = 0L
    private var mRunnable: Runnable? = null
    private lateinit var startTimeHandler: Handler

    fun setGroupId(groupId: String?) {
        this.groupId = groupId
    }

    fun setStartTime(time: Long) {
        conversationDuring = System.currentTimeMillis() - time
    }

    fun customizeChatLayout(layout: ChatView) {
        startTimeHandler = Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == MSG_TYPE.UPDATE_TITLE_MSG_TYPE.ordinal) {
                layout.titleBar.rightTitle.text = msg.obj.toString()
                layout.invalidate()
                return@Handler true
            } else if (msg.what == MSG_TYPE.CONVERSATION_WARN.ordinal) {
                return@Handler true
            } else {
                return@Handler true
            }
        }
    }

    fun startChronometer() {
        Log.e("ChatLSetting", "mChronometer start")
        mRunnable = object : Runnable {
            override fun run() {
                conversationDuring += 1
                if (conversationDuring == CONVERSATION_END) {
                    // TODO 结束会话
                    stopChronometer()
                } else if (conversationDuring >= CONVERSATION_WARN) {
                    // TODO 提醒用户
                } else {
                    // 更新计时
                    val msg = Message.obtain()
                    msg.what = MSG_TYPE.UPDATE_TITLE_MSG_TYPE.ordinal
                    msg.obj =
                        "${conversationDuring / 3600}:${conversationDuring / 60 % 60}:${conversationDuring % 60}"
                    startTimeHandler.sendMessage(msg)
                    startTimeHandler.postDelayed(this, 1000)
                }
            }
        }
        startTimeHandler.post(mRunnable!!)
    }

    fun stopChronometer(): Long {
        mRunnable?.let {
            startTimeHandler.removeCallbacks(it)
            mRunnable = null
        }
        return conversationDuring
    }

    companion object {
        private val TAG = ChatLayoutSetting::class.java.simpleName
        private const val CONVERSATION_WARN = 600L
        private const val CONVERSATION_END = 900L

        private enum class MSG_TYPE {
            UPDATE_TITLE_MSG_TYPE, CONVERSATION_WARN, CONVERSATION_END
        }
    }
}