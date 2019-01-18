package com.skide.utils

import com.skide.CoreManager
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetAddress
import java.security.MessageDigest

class LicenseChecker {


    private val licenseFile = File(File(CoreManager::class.java.protectionDomain.codeSource.location.toURI()).parent, "license")

    private fun parseFile(): Pair<String, String> {
        try {
            val obj = JSONObject(readFile(licenseFile).second)
            val keyHash = obj.getString("derivation_check")
            val key = obj.getString("key")
            return Pair(keyHash, key)
        } catch (e: Exception) {
            error("Error while checking license")
        }
    }

    private fun licenseCheck(key: String): String {
        val obj = JSONObject()
        obj.put("q", "check_license")
        obj.put("key", key)

        val request = request("http://localhost", "POST", body = obj.toString())
        val stream = ByteArrayOutputStream()
        request.third.copyTo(stream)

        return String(stream.toByteArray())
    }

    private fun getLicense(user: String, pass: String): String {
        val obj = JSONObject()
        obj.put("q", "get_license")
        obj.put("user", user)
        obj.put("pass", pass)

        val request = request("http://localhost", "POST", body = obj.toString())
        val stream = ByteArrayOutputStream()
        request.third.copyTo(stream)

        return String(stream.toByteArray())
    }

    private fun hasInternet(): Boolean {
        return try {
            InetAddress.getByName("21xayah.com").isReachable(1000)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun runCheck() {
        val hasConnection = hasInternet()
        if (licenseFile.exists()) {
            val result = parseFile()
            if (hasConnection) {
                val check = JSONObject(licenseCheck(result.second))
                if (!check.getBoolean("success")) {
                    error("Error while checking license")
                }
                val hasher = MessageDigest.getInstance("SHA-256")
                val givenHash = String(hasher.digest("${result.second}${System.getProperty("user.home")}".toByteArray()))
                if (givenHash != result.first) {
                    licenseFile.delete()
                    error("Error while checking license")
                }
            } else {
                val password = ""
                val hasher = MessageDigest.getInstance("SHA-256")
                val givenHash = String(hasher.digest("${result.second}${System.getProperty("user.home")}".toByteArray()))
                if (givenHash != result.first) {
                    error("Error while checking license")
                }
            }
        } else {
            val user = ""
            val pass = ""
            val check = JSONObject(getLicense(user, pass))
            if (check.getBoolean("success")) {
                val hasher = MessageDigest.getInstance("SHA-256")
                val license = check.getJSONObject("data").getString("license")
                val hash = String(hasher.digest("$license${System.getProperty("user.home")}".toByteArray()))
                val obj = JSONObject()
                obj.put("key", license)
                obj.put("derivation_check", hash)
                writeFile(obj.toString().toByteArray(), licenseFile, false, true)
            } else {
                error("Error while checking license")
            }

        }
    }

}