package com.skide
object Info {
    const val version = "1.4.0"
    var classLoader: ClassLoader? = null

}
fun main(args: Array<String>) = CoreManager().bootstrap(args, null)

