package com.skide.core.code.autocomplete

import netscape.javascript.JSObject

enum class CompletionType(val num:Int) {

    CLASS(6),
    COLOR(15),
    CONSTRUCTOR(3),
    ENUM(12),
    FIELD(4),
    FILE(16),
    FOLDER(18),
    FUNCTION(2),
    INTERFACE(7),
    KEYWORD(13),
    METHOD(1),
    MODULE(8),
    PROPERTY(9),
    REFERENCE(17),
    SNIPPET(14),
    TEXT(0),
    UNIT(10),
    VALUE(11),
    VARIABLE(5)

}

class AutoCompleteItem(val label:String, val kind:CompletionType, val insertText:String, val detail:String = "", val documentation:String = "") {

    fun createObject(obj:JSObject): JSObject {

        obj.setMember("kind", kind.num)
        obj.setMember("label", label)
        obj.setMember("insertText", insertText)
        obj.setMember("detail", detail)
        obj.setMember("documentation", documentation)


        return obj
    }

}