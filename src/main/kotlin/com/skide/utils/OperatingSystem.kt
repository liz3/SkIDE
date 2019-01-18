package com.skide.utils

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import java.util.*


enum class OperatingSystemType {
    MAC_OS,
    WINDOWS,
    LINUX,
    OTHER
}

fun getOS(): OperatingSystemType {

    val sys = System.getProperty("os.name")

    if (sys.contains("Windows", true)) return OperatingSystemType.WINDOWS
    if (sys.contains("Linux", true) || sys.contains("Unix", true)) return OperatingSystemType.LINUX
    if (sys.contains("Darwin", true) || sys.contains("OS X", true) || sys.contains("MAC OS", true)) return OperatingSystemType.MAC_OS

    return OperatingSystemType.OTHER
}

fun openInExplorer(f: File) {
    when (getOS()) {
        OperatingSystemType.WINDOWS -> Runtime.getRuntime().exec("explorer.exe /select,\"${f.absolutePath}\"")
        OperatingSystemType.MAC_OS ->
            Runtime.getRuntime().exec("open -R ${f.absolutePath}")
        else -> {
            if (f.isDirectory) {
                Runtime.getRuntime().exec("xdg-open ${f.absolutePath}")
            } else {
                Runtime.getRuntime().exec("xdg-open ${f.parentFile.absolutePath}")
            }
        }
    }
}

fun copyFileToClipboard(file: File) {
    val listOfFiles = ArrayList<File>()
    listOfFiles.add(file)
    val ft = FileTransferable(listOfFiles)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(ft) { _, _ -> }

}

private class FileTransferable(private val listOfFiles: List<*>) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return DataFlavor.javaFileListFlavor.equals(flavor)
    }

    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        return listOfFiles
    }
}