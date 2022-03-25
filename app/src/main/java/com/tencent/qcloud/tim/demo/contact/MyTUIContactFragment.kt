package com.tencent.qcloud.tim.demo.contact

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.qcloud.tim.demo.databinding.FragmentContactBinding
import com.tencent.qcloud.tuicore.component.fragments.BaseFragment
import com.tencent.qcloud.tuikit.tuicontact.presenter.ContactPresenter
import com.tencent.qcloud.tuikit.tuicontact.util.TUIContactLog

class MyTUIContactFragment : BaseFragment() {
    private lateinit var mBinding: FragmentContactBinding
    private val presenter: MyContactPresenter = MyContactPresenter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentContactBinding.inflate(inflater)
        initViews()
        return mBinding.root
    }

    private fun initViews() {
        // 从布局文件中获取通讯录面板
        mBinding.contactLayout.initDefault(presenter)
    }

    override fun onResume() {
        super.onResume()
        TUIContactLog.i(TAG, "onResume")
    }

    companion object {
        private val TAG = MyTUIContactFragment::class.java.simpleName
    }
}