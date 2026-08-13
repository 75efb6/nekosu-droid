package ru.nsu.ccfit.zuev.osu.helper

import org.anddev.andengine.util.Debug
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest

object MD5Calculator {
    @JvmStatic
    fun getFileMD5(file: File): String {
        var md5 = ""
        try {
            val `in` = FileInputStream(file)
            val digester = MessageDigest.getInstance("MD5")
            val bytes = ByteArray(8192)
            var byteCount: Int
            while (`in`.read(bytes).also { byteCount = it } > 0) {
                digester.update(bytes, 0, byteCount)
            }
            val hash = BigInteger(1, digester.digest())
            md5 = hash.toString(16)
            while (md5.length < 32) {
                md5 = "0$md5"
            }
            `in`.close()
        } catch (e: Exception) {
            Debug.e("MD5Calculator: " + e.message)
        }
        return md5
    }

    @JvmStatic
    fun getStringMD5(str: String): String {
        var md5 = ""
        try {
            val digester = MessageDigest.getInstance("MD5")
            digester.update(str.toByteArray())
            val hash = BigInteger(1, digester.digest())
            md5 = hash.toString(16)
            while (md5.length < 32) {
                md5 = "0$md5"
            }
        } catch (e: Exception) {
            Debug.e("MD5Calculator: " + e.message)
        }
        return md5
    }
}
