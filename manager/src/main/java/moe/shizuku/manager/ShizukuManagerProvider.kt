package moe.shizuku.manager

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import moe.shizuku.api.BinderContainer
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_TOKEN
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.server.ktx.workerHandler

class ShizukuManagerProvider : ShizukuProvider() {

    companion object {
        private const val EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER"
        private const val METHOD_SEND_USER_SERVICE = "sendUserService"
    }

    override fun onCreate(): Boolean {
        disableAutomaticSuiInitialization()
        return super.onCreate()
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (extras == null) return null

        return if (method == METHOD_SEND_USER_SERVICE) {
            try {
                extras.classLoader = BinderContainer::class.java.classLoader

                val token = extras.getString(USER_SERVICE_ARG_TOKEN) ?: return null
                val binder = extras.getParcelable<BinderContainer>(EXTRA_BINDER)?.binder ?: return null

                return runBlocking {
                    try {
                        withTimeout(5000) {
                            ShizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
                            withContext(workerHandler.asCoroutineDispatcher()) {
                                try {
                                    val reply = Bundle()
                                    Shizuku.attachUserService(binder, bundleOf(USER_SERVICE_ARG_TOKEN to token))
                                    reply!!.putParcelable(EXTRA_BINDER, BinderContainer(Shizuku.getBinder()))
                                    reply
                                } catch (e: Throwable) {
                                    Log.e("ShizukuManagerProvider", "attachUserService $token", e)
                                    null
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.e("ShizukuManagerProvider", "Binder not received in 5s", e)
                        null
                    }
                }
            } catch (e: Throwable) {
                Log.e("ShizukuManagerProvider", "sendUserService", e)
                null
            }
        } else {
            super.call(method, arg, extras)
        }
    }
}
