package com.example

import android.app.Application
import com.example.storage.TwinGuardDatabase

class TwinGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize local database
        TwinGuardDatabase.getDatabase(this)
    }
}
