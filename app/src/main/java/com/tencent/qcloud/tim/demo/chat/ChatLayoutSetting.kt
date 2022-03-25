package com.tencent.qcloud.tim.demo.chat

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.tencent.qcloud.tuikit.tuichat.ui.view.ChatView
import com.tencent.qcloud.tuikit.tuichat.ui.view.message.MessageRecyclerView

class ChatLayoutSetting {
    private var groupId: String? = null
    private var conversationDuring = 0
    private var mRunnable: Runnable? = null
    private lateinit var startTimeHandler: Handler

    fun setGroupId(groupId: String?) {
        this.groupId = groupId
    }

    fun setStartTime(time: Int) {
        conversationDuring = time
    }

    fun customizeMessageLayout(messageRecyclerView: MessageRecyclerView) {

    }

    fun customizeChatLayout(layout: ChatView) {
        startTimeHandler = Handler(Looper.getMainLooper()) { msg ->
            layout.titleBar.rightTitle.text = msg.obj.toString()
            layout.invalidate()
            return@Handler true
        }
    }

    fun startChronometer() {
        Log.e("ChatLSetting", "mChronometer start")
        mRunnable = object : Runnable {
            override fun run() {
                conversationDuring += 1
                val msg = Message.obtain()
                msg.obj = "${conversationDuring / 3600}:${conversationDuring / 60 % 60}:${conversationDuring % 60}"
                startTimeHandler.sendMessage(msg)
                startTimeHandler.postDelayed(this, 1000)
            }
        }
        startTimeHandler.post(mRunnable!!)
    }

    fun stopChronometer(): Int {
        mRunnable?.let {
            startTimeHandler.removeCallbacks(it)
            mRunnable = null
        }
        return conversationDuring
    }

    companion object {
        private val TAG = ChatLayoutSetting::class.java.simpleName
    }
}