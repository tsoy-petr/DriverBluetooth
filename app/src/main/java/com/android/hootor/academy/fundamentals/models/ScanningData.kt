package com.android.hootor.academy.fundamentals.models

sealed class ScanningData() {
    data class Barcode(val data: String): ScanningData()
    data class Error(val message: String): ScanningData()
    object Empty: ScanningData()
    object NewConnection: ScanningData()
    object HasNotBluetoothPermissions: ScanningData()
    object ShowSettingFragment: ScanningData()
    object ScanningClose: ScanningData()
}
