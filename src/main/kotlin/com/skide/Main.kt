package com.skide

import com.skide.core.debugger.Debugger

object Info {
    const val version = "1.0.3"
}

fun main(args: Array<String>) {


    Debugger()
    CoreManager().bootstrap(args)
}




