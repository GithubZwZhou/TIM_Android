package com.tencent.qcloud.tim.demo.contact

import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qcloud.tim.demo.databinding.ContactSelecableAdapterItemBinding
import com.tencent.qcloud.tuicore.TUIThemeManager
import com.tencent.qcloud.tuicore.component.imageEngine.impl.GlideEngine
import com.tencent.qcloud.tuicore.component.interfaces.IUIKitCallback
import com.tencent.qcloud.tuicore.util.ToastUtil
import com.tencent.qcloud.tuikit.tuicontact.R
import com.tencent.qcloud.tuikit.tuicontact.TUIContactService
import com.tencent.qcloud.tuikit.tuicontact.ui.view.ContactListView
import com.tencent.qcloud.tuikit.tuicontact.ui.view.ContactListView.OnSelectChangedListener

class MyContactAdapter : RecyclerView.Adapter<MyContactAdapter.ViewHolder>() {
    private val mData = ArrayList<ContactItemBeanWrapper>()
    private var mOnSelectChangedListener: OnSelectChangedListener? = null
    private var mOnClickListener: ContactListView.OnItemClickListener? = null
    private var mPreSelectedPosition = 0
    private var isSingleSelectMode = false
    private var presenter: MyContactPresenter? = null
    private var isGroupList = false

    fun setPresenter(presenter: MyContactPresenter) {
        this.presenter = presenter
    }

    fun setIsGroupList(groupList: Boolean) {
        isGroupList = groupList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.e(TAG, "creating viewHolder[$viewType]")
        return ViewHolder(
            ContactSelecableAdapterItemBinding.inflate(LayoutInflater.from(parent.context))
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactBeanWrapper = mData[position]
        Log.e(TAG, "binding viewHolder[$position] with $contactBeanWrapper")
        holder.childBinding.beanWrapper = contactBeanWrapper
        val params = holder.childBinding.viewLine.layoutParams as RelativeLayout.LayoutParams
        // tag不同时对item的分割线进行重新处理
        params.leftMargin = if (
            position < mData.size - 1
            && TextUtils.equals(contactBeanWrapper.suspensionTag, mData[position + 1].suspensionTag)
        ) {
            holder.childBinding.tvCity.left
        } else {
            0
        }
        holder.childBinding.viewLine.layoutParams = params

        if (mOnSelectChangedListener != null) {
            holder.childBinding.contactCheckBox.visibility = View.VISIBLE
            holder.childBinding.contactCheckBox.isChecked = contactBeanWrapper.bean.isSelected
        }
        holder.childBinding.selectableContactItem.setOnClickListener(View.OnClickListener {
            if (!contactBeanWrapper.bean.isEnable) {
                return@OnClickListener
            }
            holder.childBinding.contactCheckBox.isChecked =
                !holder.childBinding.contactCheckBox.isChecked
            mOnSelectChangedListener?.onSelectChanged(
                getItem(position)?.bean,
                holder.childBinding.contactCheckBox.isChecked
            )
            contactBeanWrapper.bean.isSelected = holder.childBinding.contactCheckBox.isChecked
            mOnClickListener?.onItemClick(position, contactBeanWrapper.bean)
            if (isSingleSelectMode && position != mPreSelectedPosition && contactBeanWrapper.bean.isSelected) {
                //单选模式的prePos处理
                mData[mPreSelectedPosition].bean.isSelected = false
                notifyItemChanged(mPreSelectedPosition)
            }
            mPreSelectedPosition = position
        })

        val radius =
            holder.itemView.resources.getDimensionPixelSize(R.dimen.contact_profile_face_radius)
        if (isGroupList) {
            GlideEngine.loadUserIcon(
                holder.childBinding.ivAvatar,
                contactBeanWrapper.bean.avatarUrl,
                TUIThemeManager.getAttrResId(
                    holder.childBinding.ivAvatar.context, R.attr.core_default_group_icon
                ),
                radius
            )
        } else {
            GlideEngine.loadUserIcon(
                holder.childBinding.ivAvatar,
                contactBeanWrapper.bean.avatarUrl,
                radius
            )
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        GlideEngine.clear(holder.childBinding.ivAvatar)
        holder.childBinding.ivAvatar.setImageResource(0)
        super.onViewRecycled(holder)
    }

    private fun getItem(position: Int): ContactItemBeanWrapper? {
        return if (position < mData.size) {
            mData[position]
        } else null
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setDataSource(datas: List<ContactItemBeanWrapper>) {
        mData.clear()
        mData.addAll(datas)
        Log.e(TAG, "mData.size: ${mData.size}")
        notifyDataSetChanged()
    }

    fun setSingleSelectMode(mode: Boolean) {
        isSingleSelectMode = mode
    }

    fun setOnSelectChangedListener(selectListener: OnSelectChangedListener) {
        mOnSelectChangedListener = selectListener
    }

    fun setOnItemClickListener(listener: ContactListView.OnItemClickListener) {
        mOnClickListener = listener
    }

    inner class ViewHolder(
        val childBinding: ContactSelecableAdapterItemBinding
    ) : RecyclerView.ViewHolder(childBinding.root) {

        init {
//            childBinding.conversationUnread.visibility = View.GONE
        }
    }

    companion object {
        const val TAG = "MyContactAdapter"
    }
}