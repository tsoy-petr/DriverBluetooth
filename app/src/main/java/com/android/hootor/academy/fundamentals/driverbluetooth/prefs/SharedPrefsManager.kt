package com.android.hootor.academy.fundamentals.driverbluetooth.prefs

import android.content.Context
import android.content.SharedPreferences
import com.android.hootor.academy.fundamentals.driverbluetooth.App
import com.android.hootor.academy.fundamentals.models.BlDevice

class SharedPrefsManager(private val prefs: SharedPreferences) {

    companion object {

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_MAC = "KEY_MAC"

        @Volatile
        private var INSTANCE: SharedPrefsManager? = null

        fun getInstance(): SharedPrefsManager {
            synchronized(this) {
                return INSTANCE ?: SharedPrefsManager(
                    App.INSTANCE.getSharedPreferences(
                        App.INSTANCE.packageName,
                        Context.MODE_PRIVATE
                    )
                ).also {
                    INSTANCE = it
                }
            }
        }
    }

    fun saveDevice(bluetoothDevice: BlDevice) {
        prefs.edit().apply {
            putString(KEY_TITLE, bluetoothDevice.title)
            putString(KEY_MAC, bluetoothDevice.mac)
        }.apply()
    }

    fun getDevice() : BlDevice? {
        val title = prefs.getString(KEY_TITLE, "") ?: ""
        val mac = prefs.getString(KEY_MAC, "") ?: ""
        return if (title.isNotEmpty()
            && mac.isNotEmpty()) {
            BlDevice(title, mac)
        } else null
    }

    fun getMac() = prefs.getString(KEY_MAC, "") ?: ""
}