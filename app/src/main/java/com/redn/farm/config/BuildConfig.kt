package com.redn.farm.config

import android.os.Build

object BuildConfig {
    val isAtLeastAndroid12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    // For now, we'll default to true in debug builds
    val isDebugBuild: Boolean = BuildType.current == BuildType.DEBUG
    
    object Device {
        val manufacturer: String = Build.MANUFACTURER
        val model: String = Build.MODEL
        val sdkVersion: Int = Build.VERSION.SDK_INT
        val deviceName: String = "$manufacturer $model"
    }
    
    enum class BuildType {
        DEBUG,
        RELEASE;
        
        companion object {
            val current: BuildType = DEBUG  // This should be replaced with actual build type detection
        }
    }
} 