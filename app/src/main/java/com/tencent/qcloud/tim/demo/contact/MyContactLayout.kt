package com.tencent.qcloud.tim.demo.contact

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.tencent.qcloud.tim.demo.databinding.ContactLayoutBinding
import com.tencent.qcloud.tuicore.component.TitleBarLayout

class MyContactLayout constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : LinearLayout(context, attrs, defStyleAttr), MyIContactLayout {
    private val mBinding: ContactLayoutBinding =
        ContactLayoutBinding.inflate(LayoutInflater.from(getContext()), this, true, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    fun initDefault(presenter: MyContactPresenter) {
        mBinding.contactListview.initDefault(presenter)
    }

    override fun getContactListView(): MyContactListView {
        return mBinding.contactListview
    }


    override fun getTitleBar(): TitleBarLayout? {
        return null
    }

    override fun setParentLayout(parent: Any) {}

    companion object {
        private val TAG = MyContactLayout::class.java.simpleName
    }
}