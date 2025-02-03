package com.redn.farm.config

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION;

    companion object {
        val current: Environment = if (BuildConfig.isDebugBuild) {
            DEVELOPMENT
        } else {
            PRODUCTION
        }
    }
} 