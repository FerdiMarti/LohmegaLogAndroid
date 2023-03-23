package com.example.lohmegalog.ui

data class ScanResultData(val address: String) {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ScanResultData) return false

        return this.address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}
