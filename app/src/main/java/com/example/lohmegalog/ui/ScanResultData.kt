package com.example.lohmegalog.ui

/**
 * Holds the scan result data that is necessary to display it.
 *
 * @property address - The Mac address of the device
 */
data class ScanResultData(val address: String) {

    /**
     * Override of equals function. Compares the addresses of the data
     *
     * @param other - the object to compare to
     * @return whether objects are equal
     */
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ScanResultData) return false

        return this.address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}
