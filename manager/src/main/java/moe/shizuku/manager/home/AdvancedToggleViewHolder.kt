package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeAdvancedBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import rikka.recyclerview.BaseViewHolder

/** "Advanced options": folds away the root / wireless / computer / automation cards. Data = expanded. */
class AdvancedToggleViewHolder(private val binding: HomeAdvancedBinding, root: View, onToggle: () -> Unit) :
    BaseViewHolder<Boolean>(root) {

    companion object {
        fun creator(onToggle: () -> Unit) = Creator<Boolean> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeAdvancedBinding.inflate(inflater, outer.root, true)
            AdvancedToggleViewHolder(inner, outer.root, onToggle)
        }
    }

    init {
        root.setOnClickListener { onToggle() }
    }

    override fun onBind() {
        binding.icon1.setImageResource(if (data) R.drawable.ic_expand_less_24 else R.drawable.ic_expand_more_24)
    }
}
