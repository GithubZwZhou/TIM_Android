package com.tencent.qcloud.tim.demo.contact

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.databinding.ContactListBinding
import com.tencent.qcloud.tuicore.component.CustomLinearLayoutManager
import com.tencent.qcloud.tuikit.tuicontact.TUIContactConstants
import com.tencent.qcloud.tuikit.tuicontact.TUIContactService
import com.tencent.qcloud.tuikit.tuicontact.bean.ContactItemBean
import com.tencent.qcloud.tuikit.tuicontact.component.indexlib.suspension.SuspensionDecoration
import com.tencent.qcloud.tuikit.tuicontact.ui.interfaces.IContactListView
import com.tencent.qcloud.tuikit.tuicontact.ui.pages.BlackListActivity
import com.tencent.qcloud.tuikit.tuicontact.ui.pages.FriendProfileActivity
import com.tencent.qcloud.tuikit.tuicontact.ui.pages.GroupListActivity
import com.tencent.qcloud.tuikit.tuicontact.ui.pages.NewFriendActivity
import com.tencent.qcloud.tuikit.tuicontact.ui.view.ContactListView

class MyContactListView constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), IContactListView {
    private val mBinding =
        ContactListBinding.inflate(LayoutInflater.from(getContext()), this, true, null)

    private val mContactAdapter: MyContactAdapter = MyContactAdapter()
    private val mManager: CustomLinearLayoutManager = CustomLinearLayoutManager(context)
    private val mData: ArrayList<ContactItemBeanWrapper> = ArrayList()
    private var mDecoration: SuspensionDecoration

    private var groupId = ""
    private var isGroupList = false

    private lateinit var presenter: MyContactPresenter

    private var dataSourceType = DataSource.UNKNOWN

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        inflate(context, R.layout.contact_list, this)
        mBinding.contactMemberList.apply {
            layoutManager = mManager
            adapter = mContactAdapter
            addItemDecoration(
                SuspensionDecoration(
                    context,
                    mData
                ).also { mDecoration = it })
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastCompletelyVisibleItemPosition =
                        layoutManager.findLastCompletelyVisibleItemPosition()
                    //TUILiveLog.i(TAG, "lastCompletelyVisibleItemPosition: "+lastCompletelyVisibleItemPosition);
                    if (lastCompletelyVisibleItemPosition == layoutManager.itemCount - 1) {
                        if (presenter.nextSeq > 0) {
                            presenter.loadGroupMemberList(groupId)
                        }
                    }
                }
            })
        }
        // 右侧边栏导航区域
        mBinding.contactIndexBar.setPressedShowTextView(mBinding.contactTvSideBarHint)
            .setNeedRealIndex(false)
            .setLayoutManager(mManager)
    }

    fun initDefault(presenter: MyContactPresenter) {
        this.presenter = presenter
        presenter.setContactListView(this)
        mContactAdapter.setPresenter(presenter)
        mContactAdapter.setIsGroupList(isGroupList)
        loadDataSource(DataSource.CONTACT_LIST)
        setOnItemClickListener { position, contact ->
            when (position) {
//                0 -> {
//                    val intent =
//                        Intent(TUIContactService.getAppContext(), NewFriendActivity::class.java)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    TUIContactService.getAppContext().startActivity(intent)
//                }
//                1 -> {
//                    val intent =
//                        Intent(TUIContactService.getAppContext(), GroupListActivity::class.java)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    TUIContactService.getAppContext().startActivity(intent)
//                }
//                2 -> {
//                    val intent =
//                        Intent(TUIContactService.getAppContext(), BlackListActivity::class.java)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    TUIContactService.getAppContext().startActivity(intent)
//                }
                else -> {
                    val intent =
                        Intent(TUIContactService.getAppContext(), FriendProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(TUIContactConstants.ProfileType.CONTENT, contact)
                    TUIContactService.getAppContext().startActivity(intent)
                }
            }
        }
    }

    fun setIsGroupList(isGroupList: Boolean) {
        this.isGroupList = isGroupList
    }


    override fun onDataSourceChanged(data: List<ContactItemBean>) {
        mBinding.contactLoadingBar.visibility = GONE
        mData.clear()
        mData.addAll(data.convertToWrapperList())
        Log.e(TAG, "data size: ${mData.size}")
        mContactAdapter.setDataSource(mData)
        mBinding.contactIndexBar.setSourceDatas(mData).invalidate()
        mDecoration.setDatas(mData)
    }

    override fun onFriendApplicationChanged() {
        if (dataSourceType == DataSource.CONTACT_LIST) {
            mContactAdapter.notifyItemChanged(0)
        }
    }

    fun setSingleSelectMode(mode: Boolean) {
        mContactAdapter.setSingleSelectMode(mode)
    }

    fun setOnSelectChangeListener(selectChangeListener: ContactListView.OnSelectChangedListener) {
        mContactAdapter.setOnSelectChangedListener(selectChangeListener)
    }

    private fun setOnItemClickListener(listener: ContactListView.OnItemClickListener) {
        mContactAdapter.setOnItemClickListener(listener)
    }

    private fun loadDataSource(dataSource: DataSource) {
        dataSourceType = dataSource
        Log.e(TAG, "dataSource: $dataSource")
        mBinding.contactLoadingBar.visibility = VISIBLE
        if (dataSource == DataSource.GROUP_MEMBER_LIST) {
            presenter.loadGroupMemberList(groupId)
        } else {
            presenter.loadDataSource(dataSource)
        }
    }

    fun setGroupId(groupId: String) {
        this.groupId = groupId
    }

    enum class DataSource {
        UNKNOWN, FRIEND_LIST, BLACK_LIST, GROUP_LIST, CONTACT_LIST, GROUP_MEMBER_LIST;
    }


    companion object {
        private val TAG = MyContactListView::class.java.simpleName
        private const val INDEX_STRING_TOP = "↑"
    }
}