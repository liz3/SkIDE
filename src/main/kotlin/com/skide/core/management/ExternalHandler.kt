package com.skide.core.management

import com.skide.include.OpenFileHolder
import com.skide.utils.readFile


class ExternalHandler(val openFileHolder: OpenFileHolder) {

    val content = readFile(openFileHolder.f).second

    init {
        openFileHolder.area.text = content
    }
}