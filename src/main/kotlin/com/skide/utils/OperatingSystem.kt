package com.skide.utils

enum class OperatingSystemType{
    MAC_OS,
    WINDOWS,
    LINUX,
    OTHER
}

//replace with multi variable enums - so no conditional statement is needed

fun getOS(): OperatingSystemType{
    val sys = System.getProperty("os.name")

    if (sys.contains("Windows", true)){
        return OperatingSystemType.WINDOWS
    }

    if (sys.contains("Linux", true) || sys.contains("Unix", true)){
        return OperatingSystemType.LINUX
    }

    if (sys.contains("Darwin", true) || sys.contains("OS X", true) || sys.contains("MAC OS", true)){
        return OperatingSystemType.MAC_OS
    }

    return OperatingSystemType.OTHER
}