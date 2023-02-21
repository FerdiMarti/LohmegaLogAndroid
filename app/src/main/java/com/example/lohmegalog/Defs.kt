package com.example.lohmegalog

class Defs {
    companion object {
        private fun _uuid_std(n: String): String {
            val base = "0000{}-0000-1000-8000-00805f9b34fb"
            return base.replace("{}", n)
        }

        private fun _uuid_bbl(n: String): String {
            val base = "c9f6{}-9f9b-fba4-5847-7fd701bf59f2"
            return base.replace("{}", n)
        }

        // Log (Service)
        val S_LOG = _uuid_bbl("0002")
        // Real time data characteristic (protobuf)
        val C_SENSORS_RTD = _uuid_bbl("0022")
        // Stored log characteristic (protobuf)
        val C_SENSORS_LOG = _uuid_bbl("0021")
        // Command TX characteristic (opcode, [data])
        val C_CMD_TX = _uuid_bbl("001a")
        // Command RX characteristic notification (rspcode, [data])
        val C_CMD_RX = _uuid_bbl("0023")
        // log on/off (uint32)
        val C_CFG_LOG_ENABLE = _uuid_bbl("0000")
        // bitfield (uint32)
        val C_CFG_SENSOR_ENABLE = _uuid_bbl("0001")
        // log interval in seconds (uint32)
        val C_CFG_INTERVAL = _uuid_bbl("0002")
        // rt imu mode (off = 0, 25hz = 6, 50hz = 7, 100hz = 8, 200hz = 9, 400hz = 10) (uint32)
        val C_CFG_RT_IMU = _uuid_bbl("0003")
        //
        // Device Information (Service)
        val S_DEVICE_INFORMATION = _uuid_std("180a")
        // Serial Number (String)
        val C_SERIAL_NUMBER = _uuid_std("2a25")
        // Software Revision (String)
        val C_SOFTWARE_REV = _uuid_std("2a28")
        // Manufacturer Name (String)
        val C_MANUFACTURER = _uuid_std("2a29")
        //
        // Generic Attribute Profile (Service)
        val S_GENERIC_ATTRIBUTE_PROFILE = _uuid_std("1801")
        // Service Changed ()
        val C_SERVICE_CHANGED = _uuid_std("2a05")
    }
}