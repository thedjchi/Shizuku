package moe.shizuku.manager.home.cards

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeServerStatusBinding
import moe.shizuku.manager.model.ServiceStatus
import rikka.html.text.HtmlCompat
import rikka.html.text.toHtml
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

class StatusCard
    @JvmOverloads
    constructor(
        context: Context,
    ) : MaterialCardView(context) {
        private val binding =
            HomeServerStatusBinding.inflate(
                LayoutInflater.from(context),
                this,
                true,
            )

        private val cardTitle: String
            get() = context.getString(
                R.string.home_status_service_not_running,
                context.getString(R.string.app_name)
            )
        private val cardIcon: Int
            get() = R.drawable.ic_server_error_24dp

        init {
            setTitle(cardTitle)
            setIcon(cardIcon)
        }

        private fun setTitle(text: String) {
            binding.title.text = text
        }

        private fun setSummary(text: String) {
            if (text.isEmpty()) {
                binding.summary.visibility = View.GONE
            } else {
                binding.summary.visibility = View.VISIBLE
                binding.summary.text = text
            }
        }

        private  fun setIcon(resId: Int) {
            binding.icon.setImageResource(resId)
        }

        fun update(status: ServiceStatus) {
            val ok = status.isRunning
            val isRoot = status.uid == 0
            val apiVersion = status.apiVersion
            val patchVersion = status.patchVersion
            if (ok) {
                setIcon(R.drawable.ic_server_ok_24dp)
            } else {
                setIcon(R.drawable.ic_server_error_24dp)
            }
            val user = if (isRoot) "root" else "adb"
            val title =
                if (ok) {
                    context.getString(R.string.home_status_service_is_running, context.getString(R.string.app_name))
                } else {
                    context.getString(R.string.home_status_service_not_running, context.getString(R.string.app_name))
                }
            val summary =
                if (ok) {
                    if (apiVersion != Shizuku.getLatestServiceVersion() ||
                        status.patchVersion != ShizukuApiConstants.SERVER_PATCH_VERSION
                    ) {
                        context.getString(
                            R.string.home_status_service_version_update,
                            user,
                            "$apiVersion.$patchVersion",
                            "${Shizuku.getLatestServiceVersion()}.${ShizukuApiConstants.SERVER_PATCH_VERSION}",
                        )
                    } else {
                        context.getString(R.string.home_status_service_version, user, "$apiVersion.$patchVersion")
                    }
                } else {
                    ""
                }
            binding.title.text = title.toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
            binding.summary.text = summary.toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        }
    }
