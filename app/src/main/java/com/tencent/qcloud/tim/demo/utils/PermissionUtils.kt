package com.tencent.qcloud.tim.demo.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.tencent.qcloud.tim.demo.DemoApplication
import java.util.ArrayList

object PermissionUtils {
    private val TAG = PermissionUtils::class.java.simpleName
    const val REQ_PERMISSION_CODE = 0x100

    //权限检查
    fun checkPermission(activity: Activity): Boolean {
        val permissions: MutableList<String> = ArrayList()
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                DemoApplication.mApplication!!, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                DemoApplication.mApplication!!, Manifest.permission.CAMERA
            )
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                DemoApplication.mApplication!!, Manifest.permission.RECORD_AUDIO
            )
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                DemoApplication.mApplication!!, Manifest.permission.READ_PHONE_STATE
            )
        ) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                DemoApplication.mApplication!!, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.size != 0) {
            val permissionsArray = permissions.toTypedArray()
            ActivityCompat.requestPermissions(activity, permissionsArray, REQ_PERMISSION_CODE)
            return false
        }
        return true
    }
}