package com.skide
object Info {
    const val version = "2018.2cd"
    var classLoader: ClassLoader? = null
    var prodMode = false

}
fun main(args: Array<String>) = CoreManager().bootstrap(args, null)