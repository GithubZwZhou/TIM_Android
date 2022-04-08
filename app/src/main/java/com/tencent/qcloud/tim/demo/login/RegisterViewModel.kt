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
import okhttp3.internal.commonToString
import okio.IOException

class RegisterViewModel : ViewModel() {
    private val _message = MutableLiveData<String>()
    private var cookie = ""
    val message: LiveData<String> get() = _message

    fun register(userInfo: UserInfo, verifyCode: String, whileSucceed: () -> Unit) {
        checkUserInfo(userInfo)

        val bodyMap = hashMapOf<String, String>(
            "loginName" to userInfo.phone!!,
            "password" to userInfo.password!!,
            "roleType" to "customer"
        )

        _message.postValue("注册中，请稍后！")

        HttpUtil.postRequest(
            HttpUtil.LOCAL_ADDRESS + "/api/users/create",
            bodyMap,
            object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _message.postValue(e.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val responseBody = response.body!!.string()
                    Log.e(TAG, response.headers.toString())
                    Log.e(TAG, "register ResponseBody: $responseBody")
                    if (code == 200 && Utility.checkString(responseBody, "code") == "000") {
                        val datas = Utility.checkString(responseBody, "datas")
                        if (datas == null) {
                            Log.e(TAG, "datas is null")
                            _message.postValue("datas is null")
                            return
                        }
                        userInfo.userIdLocal = Utility.checkStringFromArray(datas, "id")
                        if (userInfo.userIdLocal == null) {
                            _message.postValue("userIdLocal error")
                            return
                        }
                        localLogin(userInfo, whileSucceed)
                    } else {
                        _message.postValue(Utility.getErrorMsg(responseBody))
                    }
                }
            }
        )
    }


    fun localLogin(userInfo: UserInfo, whileSucceed: () -> Unit) {
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
                    Log.e("login response:", response.commonToString())
                    val code = response.code
                    cookie = response.header("Set-Cookie") ?: ""
                    val responseBody = response.body!!.string()
                    Log.e(TAG, "login ResponseBody: $responseBody")
                    if (code == 200) {
                        fillProfile(userInfo, whileSucceed)
                    } else {
                        _message.postValue(Utility.getErrorMsg(responseBody))
                    }
                }
            }
        )
    }

    private fun fillProfile(userInfo: UserInfo, whileSucceed: () -> Unit) {
        val bodyMap = hashMapOf<String, String>(
            "accountId" to userInfo.userIdLocal!!,
            "customerName" to userInfo.userActualName!!,
            "emergencyName" to userInfo.emergencyName!!,
            "emergencyPhone" to userInfo.emergencyNumber!!,
            "nickName" to userInfo.name!!,
            "phoneNum" to userInfo.phone!!
        )

        ToastUtil.toastShortMessage("上传用户信息中，请稍后！")

        HttpUtil.postRequest(
            HttpUtil.LOCAL_ADDRESS + "/api/customer",
            bodyMap,
            cookie,
            object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _message.postValue(e.toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    val code = response.code
                    val responseBody = response.body!!.string()
                    Log.e(TAG, "fillProfile ResponseBody: $responseBody")
                    if (code == 200 && Utility.checkString(responseBody, "code") == "000") {
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
        } else if (userInfo.emergencyName == null) {
            _message.postValue("紧急联系人姓名未填写，请重新输入！")
        } else if (userInfo.emergencyNumber == null) {
            _message.postValue("紧急联系人联系电话未填写，请重新输入！")
        } else if (userInfo.password == null) {
            _message.postValue("账号密码未填写，请重新输入！")
        } else if (userInfo.userActualName == null) {
            _message.postValue("用户真实姓名未填写，请重新输入！")
        }
    }

    private companion object {
        const val TAG = "RegisterViewModel"
    }
}

@Suppress("unchecked_cast")
class RegisterViewModelProvider : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RegisterViewModel() as T
    }

}