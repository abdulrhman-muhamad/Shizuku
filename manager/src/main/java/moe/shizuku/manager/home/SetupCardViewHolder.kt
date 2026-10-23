package moe.shizuku.manager.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeSetupBinding
import moe.shizuku.manager.setup.SetupActivity
import rikka.recyclerview.BaseViewHolder

/** Shown while the helper isn't running: the one entry point non-technical users need. */
class SetupCardViewHolder(binding: HomeSetupBinding, root: View) : BaseViewHolder<Any?>(root) {

    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeSetupBinding.inflate(inflater, outer.root, true)
            SetupCardViewHolder(inner, outer.root)
        }
    }

    init {
        val open = View.OnClickListener { v -> v.context.startActivity(Intent(v.context, SetupActivity::class.java)) }
        root.setOnClickListener(open)
        binding.button1.setOnClickListener(open)
    }
}
