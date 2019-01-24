package com.skide.utils

import com.skide.CoreManager
import com.skide.gui.LoginPrompt
import com.skide.gui.PasswordDialog
import com.skide.gui.Prompts
import javafx.scene.control.Alert
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetAddress
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*

class LicenseChecker {


    private val licenseFile = File(File(CoreManager::class.java.protectionDomain.codeSource.location.toURI()).parent, "license")

    private fun parseFile(): Pair<String, String> {
        try {
            val obj = JSONObject(readFile(licenseFile).second)
            val keyHash = String(Base64.getDecoder().decode(obj.getString("derivation_check").toByteArray()))
            val key = obj.getString("key")
            return Pair(keyHash, key)
        } catch (e: Exception) {
            error("Error while checking license")
        }
    }

    private fun licenseCheck(key: String): String {

        val request = request("https://skide.21xayah.com/web-api/?q=check_license&key=${URLEncoder.encode(key, "UTF-8")}")
        val stream = ByteArrayOutputStream()
        request.third.copyTo(stream)

        return String(stream.toByteArray())
    }

    private fun getLicense(user: String, pass: String): String {

        val request = request("https://skide.21xayah.com/web-api/?q=get_license&user=${URLEncoder.encode(user, "UTF-8")}&pass=${URLEncoder.encode(pass, "UTF-8")}")
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

    private fun askForLogin(cb: () -> Unit) {
        LoginPrompt("Login", "Login with your SkIDE Account.") { wasSuccess, user, pass ->
            if (wasSuccess) {
                val r = getLicense(user, pass)
                val check = JSONObject(r)
                if (check.getBoolean("success")) {
                    val hasher = MessageDigest.getInstance("SHA-256")
                    val license = check.getJSONObject("data").getString("license")
                    val hash = String(hasher.digest("$pass${System.getProperty("user.home")}".toByteArray()))
                    val obj = JSONObject()
                    obj.put("key", license)
                    obj.put("derivation_check", String(Base64.getEncoder().encode(hash.toByteArray())))
                    writeFile(obj.toString().toByteArray(), licenseFile, append = false, createIfNotExists = true)
                    cb()
                } else {
                    Prompts.infoCheck("Error", "Error while logging in", "The Combination of Username and Password was not found", Alert.AlertType.INFORMATION)
                    askForLogin {
                        cb()
                    }
                }
            } else {
                error("Error while checking license")
            }
        }
    }

    fun runCheck(cb: () -> Unit) {
        
        val hasConnection = hasInternet()
        if (licenseFile.exists()) {
            val result = parseFile()
            if (hasConnection) {
                val check = JSONObject(licenseCheck(result.second))
                if (!check.getBoolean("success")) {
                    licenseFile.delete()
                } else {
                    cb()
                }
            } else {
                val pass = PasswordDialog()
                val hasher = MessageDigest.getInstance("SHA-256")
                val givenHash = String(hasher.digest("$pass${System.getProperty("user.home")}".toByteArray()))
                if (givenHash != result.first) {
                    error("Error while checking license")
                } else {
                    cb()
                }
            }
        } else {
            askForLogin {
                cb()
            }
        }
    }

}