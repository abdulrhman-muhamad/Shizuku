package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import moe.shizuku.manager.HelpURLs
import moe.shizuku.manager.databinding.HomeExtraStepRequiredBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.utils.CustomTabsHelper
import rikka.recyclerview.BaseViewHolder

class AdbPermissionLimitedViewHolder(binding: HomeExtraStepRequiredBinding, root: View) : BaseViewHolder<Any?>(root) {

    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeExtraStepRequiredBinding.inflate(inflater, outer.root, true)
            AdbPermissionLimitedViewHolder(inner, outer.root)
        }
    }

    init {
        val help = HelpURLs.ADB_PERMISSION
        binding.button1.isVisible = help != null
        binding.button1.setOnClickListener { v: View -> help?.let { CustomTabsHelper.launchUrlOrCopy(v.context, it) } }
    }
}
