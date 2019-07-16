package com.skide

object Info {
    const val version = "2019.2.3"
    var prodMode = false
    var indpendentInstall = false
}

fun main(args: Array<String>) {
    CoreManager().bootstrap(args)
}