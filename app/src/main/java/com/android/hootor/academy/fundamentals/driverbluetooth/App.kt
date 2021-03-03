package com.android.hootor.academy.fundamentals.driverbluetooth

import android.app.Application

class App: Application() {

    override fun onCreate() {
        INSTANCE = this
        super.onCreate()
    }

    companion object {
        lateinit var INSTANCE: App
            private set
    }
}