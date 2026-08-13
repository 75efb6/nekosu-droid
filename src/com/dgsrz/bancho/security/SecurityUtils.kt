package com.dgsrz.bancho.security

import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.IOException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.readBytes
import ru.nsu.ccfit.zuev.osu.BuildType

object SecurityUtils {

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()
    private val BIT_MASK = intArrayOf(0x7f, 0x3f, 0x1f, 0x0f, 0x07, 0x03, 0x01)
    private val COMPRESSED = intArrayOf(
        0xd7, 0x32, 0x73, 0x0c, 0x6b, 0x96, 0x1,
        0xd4, 0xf7, 0x8, 0x36, 0xaf, 0x87, 0x0
    )  // WeLc0MeTo#0su!

    private var appSignature: String? = null
    private var secretBuffer: ByteArray? = null

    private const val flashlightCursorHash = "3b3afff3dab87f214053ded3163ff4e91cc3474e"
    private const val flashlightDimLayerHash = "59dfd45eecdfbeb7a91761a7af4b3e0162d13e9f"

    @JvmStatic
    fun getNonZeroBitsCount(x: Int): Int {
        var v = x
        v = (v and 0x55555555) + ((v and 0xaaaaaaaa.toInt()) shr 1)
        v = (v and 0x33333333) + ((v and 0xcccccccc.toInt()) shr 2)
        v = (v and 0x0f0f0f0f) + ((v and 0xf0f0f0f0.toInt()) shr 4)
        v = (v and 0x00ff00ff) + ((v and 0xff00ff00.toInt()) shr 8)
        v = (v and 0x0000ffff) + ((v and 0xffff0000.toInt()) shr 16)
        return v
    }

    @JvmStatic
    fun getHigh16Bits(x: Int): Int {
        return (x shr 12) % 3389
    }

    @JvmStatic
    fun getSecretKey(): ByteArray {
        if (secretBuffer == null) {
            var highest = 0
            secretBuffer = ByteArray(COMPRESSED.size)
            for (i in COMPRESSED.indices) {
                val index = i % 7
                if (index == 0) highest = 0
                val lowest = COMPRESSED[i] and BIT_MASK[index]
                secretBuffer!![i] = ((lowest shl index) or highest).toByte()
                highest = (COMPRESSED[i] and (0xff.toInt() xor BIT_MASK[index])) shr (7 - index)
            }
        }
        return secretBuffer!!
    }

    @JvmStatic
    fun getAppSignature(context: Context, packageName: String?) {
        if (!BuildType.hasOnlineAccess()) {
            return
        }
        if (appSignature != null || packageName == null || packageName.isEmpty()) {
            return
        }
        val pkgMgr = context.packageManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signInfo = info.signingInfo

                if (signInfo.hasMultipleSigners()) {
                    val signatures = signInfo.apkContentsSigners
                    appSignature = getHashCode(signatures[0].toByteArray())
                } else {
                    val signatures = signInfo.signingCertificateHistory
                    appSignature = getHashCode(signatures[0].toByteArray())
                }
            } else {
                @Suppress("DEPRECATION")
                val info = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                if (info != null && info.signatures != null && info.signatures.isNotEmpty()) {
                    val sign = info.signatures[0]
                    appSignature = getHashCode(sign.toByteArray())
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return
        }
    }

    @JvmStatic
    fun signRequest(request: String): String? {
        if (appSignature == null) {
            return null
        }
        try {
            val digest = "%s_%s".format(appSignature, request)
            val mac = Mac.getInstance("HmacSHA1")
            val secret = SecretKeySpec(getSecretKey(), mac.algorithm)
            mac.init(secret)
            return toHexString(mac.doFinal(digest.toByteArray()))
        } catch (e: Exception) {
            throw RuntimeException("Unsupported Algorithm")
        }
    }

    @JvmStatic
    fun verifyFileIntegrity(context: Context): Boolean {
        return verifyFileIntegrity(context, "flashlight_cursor.png", flashlightCursorHash) &&
                verifyFileIntegrity(context, "flashlight_dim_layer.png", flashlightDimLayerHash)
    }

    private fun verifyFileIntegrity(context: Context, fileName: String, hash: String): Boolean {
        try {
            context.assets.open(fileName).use { `in` ->
                val mac = Mac.getInstance("HmacSHA1")
                val secret = SecretKeySpec(getSecretKey(), mac.algorithm)
                mac.init(secret)
                val bytes = `in`.readBytes()

                return toHexString(mac.doFinal(bytes)) == hash
            }
        } catch (e: IOException) {
            Log.e("SecurityUtils", "Failed to check file integrity for $fileName", e)
            return false
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        }
    }

    private fun getHashCode(bytes: ByteArray): String {
        try {
            val digestInst = MessageDigest.getInstance("SHA1")
            digestInst.update(bytes)
            return toHexString(digestInst.digest())
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Unsupported Algorithm")
        }
    }

    private fun toHexString(bytes: ByteArray): String {
        val buffSize = bytes.size
        val sb = StringBuilder(buffSize shl 1)

        for (v in bytes) {
            sb.append(HEX_DIGITS[(v.toInt() ushr 4) and 0x0f])
            sb.append(HEX_DIGITS[v.toInt() and 0x0f])
        }
        return sb.toString()
    }
}
