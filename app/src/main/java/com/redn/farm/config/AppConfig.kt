package com.redn.farm.config

object AppConfig {
    const val APP_NAME = "YONG & EYO's FARM"
    const val APP_DESC = "RedN Farm APP allows multi-use in a farm to table setting. It has " +
            "functions for farm input recording, acquisition of product, order taking from customers, " +
            "employee compensation and other farm operation functions."
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1
    
    object Build {
        const val MIN_SDK = 25
        const val TARGET_SDK = 34
        const val COMPILE_SDK = 35
    }
    
    object Features {
        const val DYNAMIC_COLORS_ENABLED = true
        const val EDGE_TO_EDGE_ENABLED = true
    }
    
    object Debug {
        const val ENABLE_LOGGING = true
        const val ENABLE_CRASH_REPORTING = true
    }
} 