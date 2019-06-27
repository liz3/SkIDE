package com.skide

object Info {
    const val version = "2019.2.1"
    var prodMode = false
}

fun main(args: Array<String>) {
    CoreManager().bootstrap(args)
}