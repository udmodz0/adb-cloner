package com.clonespace.app

import android.app.Application
import com.clonespace.app.data.shizuku.ShizukuManager

class CloneSpaceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        ShizukuManager.cleanup()
    }
}
