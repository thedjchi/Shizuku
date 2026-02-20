package moe.shizuku.manager.management

import android.content.pm.PackageInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.databinding.AppListToggleAllBinding
import moe.shizuku.manager.management.AppsAdapter.HeaderMarker
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class ToggleAllViewHolder(private val binding: AppListToggleAllBinding) : BaseViewHolder<HeaderMarker>(binding.root), View.OnClickListener {

    companion object {
        @JvmField
        val CREATOR = Creator<HeaderMarker> { inflater: LayoutInflater, parent: ViewGroup? -> ToggleAllViewHolder(AppListToggleAllBinding.inflate(inflater, parent, false)) }
    }

    private val switchWidget get() = binding.switchWidget

    init {
        itemView.filterTouchesWhenObscured = true
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        setAllEnabled(!areAllEnabled())
        switchWidget.isChecked = areAllEnabled()
    }

    override fun onBind() {
        switchWidget.isChecked = areAllEnabled()
    }

    override fun onBind(payloads: List<Any>) {
        switchWidget.isChecked = areAllEnabled()
    }

    override fun onRecycle() {}

    private fun setAllEnabled(enabled: Boolean) {
        val items = adapter.getItems()
        for (item in items) {
            if (item is PackageInfo) {
                val pi = item
                try {
                    if (enabled) {
                        AuthorizationManager.grant(pi.packageName, pi.applicationInfo!!.uid)
                    } else {
                        AuthorizationManager.revoke(pi.packageName, pi.applicationInfo!!.uid)
                    }
                } catch (e: Exception) {}
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun areAllEnabled(): Boolean {
        val items = adapter.getItems()
        if (items.size <= 1) {
            return false
        }
        for (item in items) {
            if (item is PackageInfo) {
                val pi = item
                try {
                    if (!AuthorizationManager.granted(pi.packageName, pi.applicationInfo!!.uid)) return false
                } catch (e: Exception) {
                    return false
                }
            }
        }
        return true
    }
}
