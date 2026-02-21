package moe.shizuku.manager.home.components.dialogs

import android.os.Process
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

import moe.shizuku.manager.databinding.AboutDialogBinding
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.UpdateHelper
import moe.shizuku.manager.R

class AboutDialog(
    private val activity: ComponentActivity
) {

    fun show() {
        val binding = AboutDialogBinding.inflate(
            LayoutInflater.from(activity), null, false
        )

        binding.apply {
            icon.setImageBitmap(
                AppIconCache.getOrLoadBitmap(
                    activity,
                    activity.applicationInfo,
                    Process.myUid() / 100000,
                    activity.resources.getDimensionPixelOffset(R.dimen.default_app_icon_size),
                )
            )

            versionName.text =
                "v${activity.packageManager.getPackageInfo(activity.packageName, 0).versionName}"

            btnUpdate.setOnClickListener {
                activity.lifecycleScope.launch {
                    UpdateHelper.checkAndInstallUpdates()
                }
            }

            btnGitHub.setOnClickListener {
                CustomTabsHelper.launchUrlOrCopy(
                    activity,
                    "https://www.github.com/thedjchi/Shizuku"
                )
            }

            btnDonate.setOnClickListener {
                CustomTabsHelper.launchUrlOrCopy(
                    activity,
                    "https://www.buymeacoffee.com/thedjchi"
                )
            }

            developer.text = activity.getString(
                R.string.about_developer,
                activity.getString(R.string.about_developer_name)
            )

            fork.text = activity.getString(
                R.string.about_fork,
                activity.getString(R.string.about_fork_developer_name)
            )
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}