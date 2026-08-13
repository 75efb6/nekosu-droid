package ru.nsu.ccfit.zuev.osu.online

import com.dgsrz.bancho.security.SecurityUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import org.anddev.andengine.util.Debug
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object OnlineFileOperator {

    @JvmStatic
    fun sendFile(urlstr: String, filename: String, replayID: String) {
        try {
            val file = File(filename)
            if (!file.exists()) {
                Debug.i("$filename does not exist.")
                return
            }

            val checksum = FileUtils.getSHA256Checksum(file)
            val sb = StringBuilder()
            sb.append(URLEncoder.encode(checksum, "UTF-8"))
            sb.append("_")
            sb.append(URLEncoder.encode(replayID, "UTF-8"))
            val signature = SecurityUtils.signRequest(sb.toString())

            val mime = "application/octet-stream".toMediaType()
            val fileBody = file.asRequestBody(mime)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("uploadedfile", file.name, fileBody)
                .addFormDataPart("hash", checksum)
                .addFormDataPart("replayID", replayID)
                .addFormDataPart("sign", signature ?: "")
                .build()
            val request = Request.Builder().url(urlstr)
                .post(requestBody).build()
            val response = OnlineManager.client.newCall(request).execute()
            val responseMsg = response.body!!.string()

            Debug.i("sendFile signatureResponse $responseMsg")
        } catch (e: java.io.IOException) {
            Debug.e("sendFile IOException " + e.message, e)
        } catch (e: Exception) {
            Debug.e("sendFile Exception " + e.message, e)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun downloadFile(urlstr: String, filename: String, checkModificationDate: Boolean = false): Boolean {
        Debug.i("Starting download $urlstr")
        val file = File(filename)
        try {
            if (!checkModificationDate && file.exists()) {
                Debug.i("${file.name} already exists")
                return true
            }
            Debug.i("Connected to $urlstr")

            val builder = Request.Builder().url(urlstr)

            if (checkModificationDate && file.exists()) {
                val lastModifiedDate = Date(file.lastModified())
                val df = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
                df.timeZone = TimeZone.getTimeZone("GMT")
                builder.addHeader("If-Modified-Since", df.format(lastModifiedDate) + " GMT")
            }

            val request = builder.build()
            val response = OnlineManager.client.newCall(request).execute()

            if (!response.isSuccessful) {
                Debug.e("downloadFile failed: HTTP ${response.code} ${response.message} for $urlstr")
                response.close()
                return false
            }

            val sink = file.sink().buffer()
            sink.writeAll(response.body!!.source())
            sink.close()

            response.close()
            return true
        } catch (e: java.io.IOException) {
            Debug.e("downloadFile IOException " + e.message, e)
            return false
        } catch (e: Exception) {
            Debug.e("downloadFile Exception " + e.message, e)
            return false
        }
    }
}
