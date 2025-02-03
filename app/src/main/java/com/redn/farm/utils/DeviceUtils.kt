package com.redn.farm.utils

import android.content.Context
import android.provider.Settings

object DeviceUtils {
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
} 