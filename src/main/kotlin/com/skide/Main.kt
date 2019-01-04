package com.skide
object Info {
    const val version = "2019.1u4"
    var prodMode = false
    var serverPort = 0

}
fun main(args: Array<String>) = CoreManager().bootstrap(args)