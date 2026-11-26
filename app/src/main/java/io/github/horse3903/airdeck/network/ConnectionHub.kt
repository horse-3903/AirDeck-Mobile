package io.github.horse3903.airdeck.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import io.github.horse3903.airdeck.data.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Holds the one [ConnectionViewModel] for the whole process so the connection outlives the
 * activity. The foreground service and the UI both talk to this same instance.
 */
object ConnectionHub {
    @Volatile
    private var instance: ConnectionViewModel? = null

    fun get(context: Context): ConnectionViewModel {
        instance?.let { return it }
        return synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }
    }

    private fun create(app: Context): ConnectionViewModel {
        val repository = SettingsRepository(app)
        return ConnectionViewModel { ip, port ->
            if (repository.saveConnectionCredentials.first()) {
                repository.setLastConnectionCredentials(ip, port.toString())
            }
        }.also { it.deviceHello = helloLine(app) }
    }

    private fun clean(text: String) = text.replace('|', ' ').replace('\n', ' ').trim()

    private fun helloLine(app: Context): String {
        val name = Settings.Global.getString(app.contentResolver, "device_name")
            ?.takeIf { it.isNotBlank() } ?: Build.MODEL
        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        return "VGPHELLO:${clean(name)}|${clean(model)}\n"
    }
}
