package com.android.hootor.academy.fundamentals.settingsblutoothspp.ui

import java.util.*

object Constants {

    const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 100
    const val REQUEST_ENABLE_BT = 101

    const val SHARED_PREFERENCES_NAME = "sharedPref"

    const val KEY_NAME = "KEY_NAME"
    const val KEY_MAC = "KEY_MAC"

    val SOCKET_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_SHOW_SETTING_BLUETOOTH_FRAGMENT = "ACTION_SHOW_SETTING_BLUETOOTH_FRAGMENT"

    const val NOTIFICATION_CHANNEL_ID = "scanning_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Scanning"
    const val NOTIFICATION_ID = 1

    const val BROADCAST_ACTION = "com.hootor.borodin.broadcast.barcode.data"
}
