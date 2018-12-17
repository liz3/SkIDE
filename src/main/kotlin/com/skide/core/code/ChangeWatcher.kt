package com.skide.core.code

class ChangeWatcher(val time:Long, cb: () -> Unit) {
    var lastEdited = 0L
    init {
        Thread{
            while (true) {
                Thread.sleep(125)
                val current = System.currentTimeMillis()
                if (current - lastEdited in time..25000) {
                    cb()
                    lastEdited = 0
                }
            }
        }.start()
    }
    fun update() {
        lastEdited = System.currentTimeMillis()
    }
}