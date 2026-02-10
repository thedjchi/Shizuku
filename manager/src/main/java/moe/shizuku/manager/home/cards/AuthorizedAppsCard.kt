package moe.shizuku.manager.home.cards

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import moe.shizuku.manager.R
import moe.shizuku.manager.management.ApplicationManagementActivity

class AuthorizedAppsCard
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : BaseCard(context, attrs) {
        override val cardTitle: String
            get() = context.getString(R.string.home_app_management_title)
        override val cardIcon: Int
            get() = R.drawable.ic_settings_outline_24dp

        fun update(grantedCount: Int) {
            setTitle(
                context.resources.getQuantityString(
                    R.plurals.home_app_management_authorized_apps_count,
                    grantedCount,
                    grantedCount,
                ),
            )
        }

        override fun onClick(v: View) {
            v.context.startActivity(Intent(v.context, ApplicationManagementActivity::class.java))
        }
    }
