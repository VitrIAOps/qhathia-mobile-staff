package com.qhathia.mobile.staff.core.data.local

import android.os.Build
import javax.inject.Inject

/** Names this phone for the backend's session list ("Xiaomi Redmi Note 12"). */
fun interface DeviceLabelProvider {
    fun label(): String?
}

/** [DeviceLabelProvider] reading the manufacturer and model from [Build]. */
class BuildDeviceLabelProvider @Inject constructor() : DeviceLabelProvider {
    override fun label(): String? = deviceLabel(Build.MANUFACTURER, Build.MODEL)
}

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
