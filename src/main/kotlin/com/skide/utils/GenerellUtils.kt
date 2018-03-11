package com.skide.utils

fun adjustVersion(value:String): String {

    var str = value.replace("-dev", ".")
    var fails = 0
    while (true) {
        try {
            Integer.parseInt(str.replace(".", ""))
            break
        }catch (e:Exception) {
            str = str.substring(0, str.length - 1)
            fails++
        }
    }
    if(str.length == 3) str += ".0"
    return if(fails == 0) str else "$str$fails"
}