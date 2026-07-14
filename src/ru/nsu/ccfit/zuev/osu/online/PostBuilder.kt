package ru.nsu.ccfit.zuev.osu.online

import com.dgsrz.bancho.security.SecurityUtils
import okhttp3.FormBody
import okhttp3.Request
import org.anddev.andengine.util.Debug
import java.io.BufferedReader
import java.io.StringReader
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.ArrayList

class PostBuilder {
    private val formBodyBuilder = FormBody.Builder()
    private val values = StringBuilder()

    fun addParam(key: String, value: String) {
        try {
            if (values.length > 0) {
                values.append("_")
            }
            formBodyBuilder.add(key, value)
            values.append(URLEncoder.encode(value, "UTF-8"))
        } catch (e: java.io.UnsupportedEncodingException) {
            return
        }
    }

    @Throws(RequestException::class)
    fun requestWithAttempts(scriptUrl: String, attempts: Int): ArrayList<String> {
        var response: ArrayList<String>? = null
        val signature = SecurityUtils.signRequest(values.toString())

        if (signature != null) {
            addParam("sign", signature)
        }
        for (i in 0 until attempts) {
            try {
                response = request(scriptUrl)
            } catch (e: RequestException) {
                if (e.cause is UnknownHostException) {
                    Debug.e("Cannot resolve server name")
                    break
                }
                Debug.e("Received error, continuing... ", e)
                response = null
            }

            if (response == null || response.isEmpty() || response[0].isEmpty()
                || !(response[0] == "FAIL" || response[0] == "SUCCESS")
            ) {
                try {
                    Thread.sleep(3000)
                } catch (e: InterruptedException) {
                }
                continue
            }
            break
        }

        val result = response ?: ArrayList()

        if (result.isEmpty()) {
            result.add("")
        }
        return result
    }

    @Throws(RequestException::class)
    private fun request(scriptUrl: String): ArrayList<String> {
        val response = ArrayList<String>()

        try {
            val request = Request.Builder()
                .url(scriptUrl)
                .post(formBodyBuilder.build())
                .build()
            val resp = OnlineManager.client.newCall(request).execute()

            Debug.i("request url=$scriptUrl")
            Debug.i("request --------Content---------")
            val reader = BufferedReader(StringReader(resp.body!!.string()))
            var line: String? = null
            while (reader.readLine().also { line = it } != null) {
                Debug.i(String.format("request [%d]: %s", response.size, line))
                response.add(line!!)
            }
            Debug.i("request url=$scriptUrl")
            Debug.i("request -----End of content-----")
        } catch (e: Exception) {
            Debug.e(e.message, e)
        }

        if (response.isEmpty()) {
            response.add("")
        }
        return response
    }

    class RequestException(cause: Throwable) : Exception(cause) {
        companion object {
            private const val serialVersionUID = 671773899432746143L
        }
    }
}
