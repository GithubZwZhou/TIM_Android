package com.tencent.qcloud.tim.demo.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object Utility {
    //返回Json数据的特定String值
    fun checkString(response: String, string: String): String? {
        try {
            val dataObject = JSONObject(response)
            return dataObject.getString(string)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun getErrorMsg(response: String): String {
        try {
            val dataObject = JSONObject(response)
            return dataObject.getString("msg") ?: "未知错误"
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return "未知错误"
    }

    fun checkStringFromArray(response: String, string: String): String? {
        try {
            val dataArray = JSONArray(response)
            return checkString(dataArray[0].toString(), string)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }
}