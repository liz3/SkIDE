package com.skide.utils

enum class OperatingSystemType {
    MAC_OS,
    WINDOWS,
    LINUX,
    OTHER
}

fun getOS(): OperatingSystemType {

    val sys = System.getProperty("os.name")

    if (sys.contains("Windows", true)) return OperatingSystemType.WINDOWS
    if (sys.contains("Linux", true) || sys.contains("Unix", true)) return OperatingSystemType.LINUX
    if (sys.contains("Darwin", true) || sys.contains("OSX", true) || sys.contains("macos", true)) return OperatingSystemType.MAC_OS

    return OperatingSystemType.OTHER
}