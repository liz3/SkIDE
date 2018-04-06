package com.skide.core.code

import javafx.application.Platform
import org.fxmisc.richtext.CodeArea

class ChangeWatcher(val area: CodeArea, val wait: Long, val caller: () -> Unit){
    var lastEdited = 0L
    
    private val th = Thread{
        while (true){
            Thread.sleep(125)

            val current = System.currentTimeMillis()

            if(current - lastEdited in wait..25000){
                caller()

                lastEdited = 0
            }
        }
    }

    fun start(){
        area.textProperty().addListener{ _, _, _ ->
            Platform.runLater{
                lastEdited = System.currentTimeMillis()
            }
        }

        th.start()
    }

    init{
        //unused
    }
}