package com.qhathia.mobile.staff.core.data.local

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Names this phone for the backend's session list ("Xiaomi Redmi Note 12"). */
fun interface DeviceLabelProvider {
    fun label(): String?
}

/**
 * [DeviceLabelProvider] preferring the name the user gave the phone ("Galaxy A54 de Carla", in
 * Settings › About phone), and falling back to the manufacturer and model from [Build] (UX-C-17).
 * Some vendors leave the name empty or refuse to read it; both fall back.
 */
class BuildDeviceLabelProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceLabelProvider {
    override fun label(): String? {
        val userName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()
        return preferredDeviceLabel(userName, Build.MANUFACTURER, Build.MODEL)
    }
}

/** The user's device name when there is one, else [deviceLabel] of the maker and model. */
internal fun preferredDeviceLabel(deviceName: String?, manufacturer: String?, model: String?): String? =
    deviceName?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_DEVICE_LABEL_LENGTH)
        ?: deviceLabel(manufacturer, model)

private const val MAX_DEVICE_LABEL_LENGTH = 100

/**
 * "Xiaomi" + "Redmi Note 12" → "Xiaomi Redmi Note 12"; "samsung" + "Samsung Galaxy S23" →
 * "Samsung Galaxy S23" (the model already names the maker). Blank → `null`; max 100 characters.
 */
internal fun deviceLabel(manufacturer: String?, model: String?): String? {
    val maker = manufacturer?.trim().orEmpty().replaceFirstChar { it.uppercaseChar() }
    val device = model?.trim().orEmpty()
    val label = when {
        device.isEmpty() -> maker
        maker.isEmpty() || device.startsWith(maker, ignoreCase = true) -> device
        else -> "$maker $device"
    }
    return label.takeIf { it.isNotBlank() }?.take(MAX_DEVICE_LABEL_LENGTH)
}
