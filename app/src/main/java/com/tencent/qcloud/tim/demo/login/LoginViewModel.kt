package com.tencent.qcloud.tim.demo.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tencent.qcloud.tim.demo.bean.UserInfo
import com.tencent.qcloud.tim.demo.utils.HttpUtil
import com.tencent.qcloud.tim.demo.utils.Utility
import com.tencent.qcloud.tuicore.util.ToastUtil
import okhttp3.Call
import okhttp3.Response
import okio.IOException

class LoginViewModel : ViewModel() {
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    fun localLogin(userInfo: UserInfo, whileSucceed: () -> Unit) {
        checkUserInfo(userInfo)
        val bodyMap = hashMapOf<String, String>(
            "username" to userInfo.phone!!,
            "password" to userInfo.password!!,
        )

        _message.postValue("登录中，请稍后！")

        HttpUtil.postRequest(
            HttpUtil.LOCAL_ADDRESS + "/api/users/login",
            bodyMap,
            object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _message.postValue(e.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val responseBody = response.body!!.string()
                    Log.e(TAG, "login ResponseBody: $responseBody")
                    if (code == 200) {
                        whileSucceed()
                    } else {
                        _message.postValue(Utility.getErrorMsg(responseBody))
                    }
                }
            }
        )
    }

    private fun checkUserInfo(userInfo: UserInfo) {
        if (userInfo.phone == null) {
            _message.postValue("用户手机号未填写，请重新输入！")
        } else if (userInfo.password == null) {
            _message.postValue("账号密码未填写，请重新输入！")
        }
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}

@Suppress("unchecked_cast")
class LoginViewModelProvider : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel() as T
    }

}