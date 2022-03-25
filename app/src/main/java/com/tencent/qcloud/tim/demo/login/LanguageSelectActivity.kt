package com.tencent.qcloud.tim.demo.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qcloud.tim.demo.R
import com.tencent.qcloud.tim.demo.databinding.ActivityThemeLanguageSelectBinding
import com.tencent.qcloud.tuicore.TUIThemeManager
import com.tencent.qcloud.tuicore.component.CustomLinearLayoutManager
import com.tencent.qcloud.tuicore.component.activities.BaseLightActivity
import com.tencent.qcloud.tuicore.component.interfaces.ITitleBarLayout
import java.util.*

class LanguageSelectActivity : BaseLightActivity() {
    private lateinit var mBinding: ActivityThemeLanguageSelectBinding

    private lateinit var onItemClickListener: OnItemClickListener
    private val languageMap: MutableMap<String, String> = HashMap()
    private val languageList: MutableList<String> = ArrayList()
    private lateinit var adapter: SelectAdapter
    private lateinit var currentLanguage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityThemeLanguageSelectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mBinding.demoSelectTitleBar.setTitle(
            resources.getString(R.string.demo_language_title),
            ITitleBarLayout.Position.MIDDLE
        )
        mBinding.demoSelectTitleBar.leftGroup.setOnClickListener { finish() }

        currentLanguage = TUIThemeManager.getInstance().currentLanguage
        if (TextUtils.isEmpty(currentLanguage)) {
            val locale: Locale = resources.configuration.locales[0]
            currentLanguage = locale.language
        }
        adapter = SelectAdapter()
        initData()
        mBinding.themeRecyclerView.adapter = adapter
        mBinding.themeRecyclerView.layoutManager = CustomLinearLayoutManager(this)

        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ResourcesCompat.getDrawable(resources, R.drawable.core_list_divider, null)?.let {
            dividerItemDecoration.setDrawable(it)
        }
        mBinding.themeRecyclerView.addItemDecoration(dividerItemDecoration)
        onItemClickListener = object : OnItemClickListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onClick(language: String) {
                currentLanguage = if (TextUtils.equals(currentLanguage, language)) {
                    return
                } else {
                    language
                }
                val index = if (TextUtils.equals(language, "zh")) 0 else 1
                adapter.setSelectedItem(index)
                adapter.notifyDataSetChanged()
                TUIThemeManager.getInstance()
                    .changeLanguage(this@LanguageSelectActivity, currentLanguage)
                changeTitleLanguage()
                notifyLanguageChanged()
            }
        }
    }

    private fun notifyLanguageChanged() {
        val intent = Intent()
        intent.action = DEMO_LANGUAGE_CHANGED_ACTION
        intent.putExtra(LANGUAGE, currentLanguage)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun changeTitleLanguage() {
        mBinding.demoSelectTitleBar.setTitle(
            resources.getString(R.string.demo_language_title),
            ITitleBarLayout.Position.MIDDLE
        )
    }

    private fun initData() {
        val simplifiedChinese = "简体中文"
        val english = "English"
        languageList.add(simplifiedChinese)
        languageMap[simplifiedChinese] = "zh"
        languageList.add(english)
        languageMap[english] = "en"
        if (TextUtils.equals(currentLanguage, "zh")) {
            adapter.setSelectedItem(0)
        } else {
            adapter.setSelectedItem(1)
        }
    }

    internal inner class SelectAdapter : RecyclerView.Adapter<SelectAdapter.SelectViewHolder>() {
        private var selectedItem = -1

        fun setSelectedItem(selectedItem: Int) {
            this.selectedItem = selectedItem
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder {
            val view = LayoutInflater.from(this@LanguageSelectActivity)
                .inflate(R.layout.core_select_item_layout, parent, false)
            return SelectViewHolder(view)
        }

        override fun onBindViewHolder(holder: SelectViewHolder, position: Int) {
            val language = languageList[position]
            holder.name.text = language
            if (selectedItem == position) {
                holder.selectedIcon.visibility = View.VISIBLE
            } else {
                holder.selectedIcon.visibility = View.GONE
            }
            holder.itemView.setOnClickListener { onItemClickListener.onClick(languageMap[language]!!) }
        }

        override fun getItemCount(): Int {
            return languageMap.size
        }

        internal inner class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var name: TextView = itemView.findViewById(com.tencent.qcloud.tuicore.R.id.name)
            var selectedIcon: ImageView =
                itemView.findViewById(com.tencent.qcloud.tuicore.R.id.selected_icon)

        }
    }

    interface OnItemClickListener {
        fun onClick(language: String)
    }

    companion object {
        const val LANGUAGE = "language"
        const val DEMO_LANGUAGE_CHANGED_ACTION = "demoLanguageChangedAction"

        @JvmStatic
        fun startSelectLanguage(activity: Activity) {
            val intent = Intent(activity, LanguageSelectActivity::class.java)
            activity.startActivity(intent)
        }
    }
}