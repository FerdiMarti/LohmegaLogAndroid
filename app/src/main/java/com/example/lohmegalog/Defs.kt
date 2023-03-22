package com.example.lohmegalog

import java.util.Dictionary

const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

class UUIDS {
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

        //Battery Service
        val S_BATTERY = _uuid_std("180f")
        // Battery Level
        val C_BATTERY_LEVEL = _uuid_std("2a19")
    }
}

class CMD_OPCODE {
    //Command (request) codes (aka 'BB_LOG_CMD_...')

    companion object {
        // fmt: off
        val UPDATE_READ_PTR       =  0x00
        val BLINK_LED             =  0x01
        val ENTER_DFU             =  0x02
        val CALIBRATE_GYRO        =  0x03
        val CALIBRATE_COMPASS     =  0x04
        val CALIBRATE_END         =  0x05
        val  SET_PASSCODE         =  0x06
        val GET_PASSCODE_STATE    =  0x07
        val SET_DISABLE_CAL_CORR  =  0x08
        val  GET_DISABLE_CAL_CORR =  0x09
        val CAL_CLEAR_TEMP_LUT    =  0x0A
        val CAL_SET_TEMP_LUT_VAL  =  0x0B
        val CAL_SAVE_TEMP_LUT     =  0x0C
        val UPDATE_GET_MEM        =  0x70
        // fmt: on
    }
}

class CMD_RESP {
    //Command response codes. (aka 'RESP_...')

    companion object {
        //fmt: off
        val SUCCESS                     =  0x00
        val ERROR                       =  0x01
        val ERROR_PASSCODE_FORMAT       =  0x02
        val ERROR_COMPASS_NO_MOTION     =  0x03
        val ERROR_COMPASS_LARGE_MAGNET  =  0x04
        val ERROR_ACCESS_DENIED         =  0x05
        val ERROR_UNKNOWN_CMD           =  0x06
        val COMPLETE                    =  0x80
        val ERROR_CALIBRATION           =  0x81
        val PROGRESS                    =  0x82
        // fmt: on
    }
}


class PASSCODE_STATUS {
    // Password status codes (aka 'BB_PASSCODE_...')

    companion object {
        // fmt: off
        val INIT       = 0x00 // the unit has not been configured yet
        val UNVERIFIED = 0x01 // correct password has not been entered yet
        val VERIFIED   = 0x02 // correct password has been entered
        val DISABLED   = 0x03 // no password is needed
        // fmt: on
    }
}

class _BlueBerryLogEntryField {

    var enmask: Int? = null
    var pbname: String = ""
    var symbol: String = ""
    var unit: String = ""
    var tounit: ((Float) -> Float) = {x -> x}
    var alias: String = ""
    var subfields: List<String>? = null
    var txtfmt: String = "4.3f"
    var apiname: String = ""
    var colnames: ArrayList<String> = ArrayList()

    /*
        Args:
            enmask: enable bit mask
            pbname: protobuf descriptor field name
            symbol: SI symbol or similar identifier
            tounit: func to convert from raw value
     */

    constructor (enmask: Int?, pbname: String, symbol: String, unit: String, tounit: (Float) -> Float, alias: String?, subfields: List<String>?, txtfmt: String?){
        this.enmask = enmask
        this.pbname = pbname
        this.symbol = symbol
        this.unit = unit
        this.tounit = tounit
        this.txtfmt = "{{0: %s}}".format(txtfmt)

        if (alias != null) {
            this.apiname = alias
        } else {
            this.apiname = pbname
        }

        if (subfields != null) {
            for (x in subfields) {
                this.colnames.add("%s_%s".format(this.symbol, x))
            }
        } else {
            this.colnames.add(this.symbol)
        }

        fun isConfigurable(): Boolean {
            return (this.enmask == null)
        }
    }
}


class BlueBerryLogEntryFields {
    /*
    Log entry data field - i.e. a sensor value in most cases.
    Names used in app csv output:
        Unix time stamp,
        Acceleration x (m/s²),
        Acceleration y (m/s²),
        Acceleration z (m/s²),
        Magnetic field x (µT),
        Magnetic field y (µT),
        Magnetic field z (µT),
        Rotation rate x (°/s),
        Rotation rate y (°/s),
        Rotation rate z (°/s),
        Illuminance (lux),
        Pressure (hPa),
        Rel. humidity (%),
        Temperature (C),
        UV index,
        Battery voltage (V)
    */

    companion object {

        val PRESSURE = _BlueBerryLogEntryField(
            enmask=0x0001,
            pbname="pressure",
            symbol="p",
            unit="hPa",
            tounit={x -> (x / 100.0).toFloat() },
            alias= null,
            subfields=null,
            txtfmt=null
        )

        val HUMIDITY = _BlueBerryLogEntryField(
            enmask=0x0002,
            pbname="rh",
            symbol="rh",
            unit="%",
            tounit={x -> (x / 10.0).toFloat()},
            alias="humid",
            subfields=null,
            txtfmt=null
        )

        val TEMPERATURE = _BlueBerryLogEntryField(
            enmask=0x0004,
            pbname="temperature",
            symbol="t",
            unit="C",
            tounit={x -> (x / 1000.0).toFloat()},
            alias="temp",
            subfields=null,
            txtfmt=null
        )

        val COMPASS = _BlueBerryLogEntryField(
            enmask=0x0008,
            pbname="compass",
            symbol="m",
            unit="µT",
            tounit={x -> (x * 4915.0 / 32768.0).toFloat()},
            alias=null,
            subfields=listOf("x", "y", "z"),
            txtfmt=null
        )

        val ACCELEROMETER = _BlueBerryLogEntryField(
            enmask=0x0010,
            pbname="accelerometer",
            symbol="a",
            unit="m/s²",
            tounit={x -> (x * 2.0 * 9.81 / 32768.0).toFloat()},
            alias="accel",
            subfields=listOf("x", "y", "z"),
            txtfmt=null
        )

        val GYRO = _BlueBerryLogEntryField(
            enmask=0x0020,
            pbname="gyro",
            symbol="g",
            unit="°/s",
            tounit={x -> (x * 250.0 / 32768.0).toFloat()},
            alias=null,
            subfields=listOf("x", "y", "z"),
            txtfmt=null
        )

        val LUX = _BlueBerryLogEntryField(
            enmask=0x0040,
            pbname="lux",
            symbol="L",
            unit="lux",
            tounit={x -> (x / 1000.0).toFloat()},
            // alias="illuminance"
            subfields=null,
            alias=null,
            txtfmt=null,
        )

        val UVI = _BlueBerryLogEntryField(
            enmask=0x0100,
            pbname="uvi",
            symbol="UVi",
            unit="",  // FIXME
            tounit={x-> (x / 1000.0).toFloat()},
            alias=null,
            subfields=null,
            txtfmt=null
        )

        val BATVOLT = _BlueBerryLogEntryField(
            enmask=0x0200,
            pbname="battery_mv",
            symbol="bat",
            unit="V",
            tounit={x -> (x / 1000.0).toFloat()},
            alias="batvolt",
            subfields = null,
            txtfmt = null
        )

        val TIME = _BlueBerryLogEntryField(
            enmask=null,
            pbname="timestamp",
            symbol="TS",
            unit="s",
            tounit={x -> (x).toFloat()},
            alias=null,
            txtfmt="7.0f",
            subfields = null,
        )

        val _GPIO0ADC = _BlueBerryLogEntryField(
            enmask=null,
            pbname="gpio0_mv",
            symbol="gp0",
            unit="mV",
            tounit={x -> (x * 1.0).toFloat()},
            alias=null,
            subfields = null,
            txtfmt = null
        )

        val _GPIO1ADC = _BlueBerryLogEntryField(
            enmask=null,
            pbname="gpio1_mv",
            symbol="gp1",
            unit="mV",
            tounit={x -> (x * 1.0).toFloat()},
            alias=null,
            subfields = null,
            txtfmt = null
        )

        val _INT_GPIO0 = _BlueBerryLogEntryField(
            enmask=null,
            pbname="int_gpio0",
            symbol="int0",
            unit="",
            tounit={x -> x},
            alias=null,
            subfields = null,
            txtfmt = null
        )

        val _INT_GPIO1 = _BlueBerryLogEntryField(
            enmask=null,
            pbname="int_gpio1",
            symbol="int1",
            unit="",
            tounit={x -> x},
            alias=null,
            subfields = null,
            txtfmt = null
        )

        val _INT_ACC = _BlueBerryLogEntryField(
            enmask= null,
            pbname="int_acc",
            symbol="iacc1",
            unit="",
            tounit={x -> x},
            alias=null,
            subfields = null,
            txtfmt = null
        )

        val fieldMap = mapOf(
            PRESSURE.pbname to PRESSURE,
            HUMIDITY.pbname to HUMIDITY,
            TEMPERATURE.pbname to TEMPERATURE,
            COMPASS.pbname to COMPASS,
            ACCELEROMETER.pbname to ACCELEROMETER,
            GYRO.pbname to GYRO,
            LUX.pbname to LUX,
            UVI.pbname to UVI,
            BATVOLT.pbname to BATVOLT,
            TIME.pbname to TIME,
        )
    }
}