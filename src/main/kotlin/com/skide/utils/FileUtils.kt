package com.skide.utils

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

enum class FileReturnResult{
    ERROR,
    SUCCESS,
    IS_DIR,
    NOT_FOUND
}

fun readFile(path: String) = readFile(File(path))

fun writeFile(data: ByteArray, path: String, append: Boolean = false, createIfNotExists: Boolean = false) = writeFile(data, File(path), append, createIfNotExists)

fun writeFile(data: ByteArray, path: String) = writeFile(data, File(path))

fun readFile(file: File): Pair<FileReturnResult, String>{
    if (!file.exists()){
        return Pair(FileReturnResult.NOT_FOUND, "")
    }

    if (file.isDirectory){
        return Pair(FileReturnResult.IS_DIR, "")
    }

    return try{
        val array = Files.readAllBytes(file.toPath())

        Pair(FileReturnResult.SUCCESS, String(array, Charset.forName("UTF-8")))
    }catch (e: Exception){
        e.printStackTrace()

        Pair(FileReturnResult.ERROR, "")
    }
}

fun writeFile(data: ByteArray, file: File, append: Boolean = false, createIfNotExists: Boolean = false): Pair<FileReturnResult, String>{
    if (!createIfNotExists && !file.exists()){
        return Pair(FileReturnResult.NOT_FOUND, "")
    }

    if (createIfNotExists && !file.exists()){
        val created = file.createNewFile()

        if (!created){
            return Pair(FileReturnResult.ERROR, "")
        }
    }

    return try{
        if (append) {
            Files.write(Paths.get(file.absolutePath), data, StandardOpenOption.APPEND)
        }else {
            Files.write(Paths.get(file.absolutePath), data, StandardOpenOption.TRUNCATE_EXISTING)
        }

        Pair(FileReturnResult.SUCCESS, "")
    }catch (e: Exception){
        e.printStackTrace()

        Pair(FileReturnResult.ERROR, "")
    }
}