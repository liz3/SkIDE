package com.skide

object Info {
    const val version = "2019.1u6"
    var prodMode = false
}
fun main(args: Array<String>) {
    CoreManager().bootstrap(args)
}

class ReflectionLauncher {
    fun start(args:Array<String>) {
       CoreManager().bootstrap(args)
   }
}