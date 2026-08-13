package ru.nsu.ccfit.zuev.osu

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.discord.DiscordRPC
import org.apache.http.HttpException
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.lang.reflect.Field
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Date
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class AppException : Exception, Thread.UncaughtExceptionHandler {
    private var type: Byte = 0
    private var code: Int = 0
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    private constructor() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    private constructor(type: Byte, code: Int, excp: Exception?) : super(excp) {
        this.type = type
        this.code = code
        if (Debug) {
            saveErrorLog(excp!!)
        }
    }

    fun getCode(): Int = code

    fun getType(): Byte = type

    fun makeToast(ctx: Context) {
        when (type) {
            TYPE_HTTP_CODE -> {
                val err = ctx.getString(R.string.http_status_code_error, code)
                Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
            }
            TYPE_HTTP_ERROR -> Toast.makeText(ctx, R.string.http_exception_error, Toast.LENGTH_SHORT).show()
            TYPE_SOCKET -> Toast.makeText(ctx, R.string.socket_exception_error, Toast.LENGTH_SHORT).show()
            TYPE_NETWORK -> Toast.makeText(ctx, R.string.network_not_connected, Toast.LENGTH_SHORT).show()
            TYPE_XML -> Toast.makeText(ctx, R.string.xml_parser_failed, Toast.LENGTH_SHORT).show()
            TYPE_IO -> Toast.makeText(ctx, R.string.io_exception_error, Toast.LENGTH_SHORT).show()
            TYPE_RUN -> Toast.makeText(ctx, R.string.app_run_code_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun saveErrorLog(excp: Exception) {
        saveErrorLog(excp.localizedMessage)
    }

    fun saveErrorLog(excpMessage: String?) {
        val errorlog = "errorlog.txt"
        var savePath = ""
        var logFilePath = ""
        var fw: FileWriter? = null
        var pw: PrintWriter? = null
        try {
            val storageState = Environment.getExternalStorageState()
            if (storageState == Environment.MEDIA_MOUNTED) {
                savePath = Config.getCorePath() + File.separator + "Log/"
                val file = File(savePath)
                if (!file.exists()) {
                    file.mkdirs()
                }
                logFilePath = savePath + errorlog
            }
            if (logFilePath == "") {
                return
            }
            val logFile = File(logFilePath)
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            fw = FileWriter(logFile, true)
            pw = PrintWriter(fw)
            pw.println("--------------------" + DateFormat.format("yyyy-MM-dd hh:mm:ss", Date()) + "---------------------")
            pw.println(excpMessage)
            pw.close()
            fw.close()
        } catch (e: Exception) {
            Log.e("AppException", "[Exception]${e.localizedMessage}")
        } finally {
            pw?.close()
            fw?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                }
            }
        }
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(thread, ex)
        } else {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (Multiplayer.isMultiplayer) Multiplayer.log("CRASH")
            SaveServiceObject.finishAllActivities()
            android.os.Process.killProcess(android.os.Process.myPid())
            DiscordRPC.clear()
            System.exit(1)
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        val context = GlobalManager.getInstance().getMainActivity() ?: return false

        if (Multiplayer.isMultiplayer) Multiplayer.log(ex)

        val crashReport = getCrashReport(context, ex)
        Thread {
            Looper.prepare()
            Toast.makeText(context, StringTable.get(R.string.crash), Toast.LENGTH_SHORT).show()
            Looper.loop()
        }.start()

        saveErrorLog(crashReport)
        return true
    }

    private fun getCrashReport(context: Context, ex: Throwable): String {
        val pm = context.packageManager
        val exceptionStr = StringBuilder()
        try {
            val pinfo = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            exceptionStr.append("Version: ").append(pinfo.versionName).append("(").append(pinfo.versionCode).append(")\n\n")
            exceptionStr.append("Android: ").append(Build.VERSION.RELEASE).append("(").append(Build.MODEL).append(")\n\n")
            exceptionStr.append("System Package Info:").append(collectDeviceInfo(context)).append("\n\n")
            exceptionStr.append("System Screen Info:").append(getScreenInfo(context)).append("\n\n")
            exceptionStr.append("System OS Info:").append(getMobileInfo()).append("\n\n")
            exceptionStr.append("Exception: ").append(ex.message).append("\n\n")
            exceptionStr.append("Exception stack：").append(getTraceInfo(context, ex)).append("\n\n")
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        ex.printStackTrace()
        return exceptionStr.toString()
    }

    fun collectDeviceInfo(ctx: Context): String {
        val sb = StringBuilder()
        val activePackageJson = JSONObject()
        try {
            val pm = ctx.packageManager
            val pi = pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)
            if (pi != null) {
                val versionName = pi.versionName ?: "null"
                val versionCode = pi.versionCode.toString()
                activePackageJson.put("versionName", versionName)
                activePackageJson.put("versionCode", versionCode)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppException", "an error occured when collect package info", e)
        } catch (e: JSONException) {
            Log.e("AppException", "jsonException", e)
        }
        sb.append("[active Package]")
        sb.append(activePackageJson.toString())
        return sb.toString()
    }

    fun getMobileInfo(): String {
        val osJson = JSONObject()
        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                osJson.put(field.name, field.get(null).toString())
                Log.d("AppException", "${field.name} : ${field.get(null)}")
            } catch (e: Exception) {
                Log.e("AppException", "an error occured when collect crash info", e)
            }
        }
        return try {
            osJson.toString(4)
        } catch (e: JSONException) {
            e.printStackTrace()
            osJson.toString()
        }
    }

    fun getScreenInfo(ctx: Context): String {
        val osJson = JSONObject()
        val displaymetrics = DisplayMetrics()
        (ctx.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.getMetrics(displaymetrics)
        for (field in displaymetrics.javaClass.declaredFields) {
            try {
                field.isAccessible = true
                osJson.put(field.name, field.get(displaymetrics).toString())
                Log.d("AppException", "${field.name} : ${field.get(displaymetrics)}")
            } catch (e: Exception) {
                Log.e("AppException", "an error occured when collect crash info", e)
            }
        }
        return try {
            osJson.toString(4)
        } catch (e: JSONException) {
            e.printStackTrace()
            osJson.toString()
        }
    }

    companion object {
        @JvmField
        val TYPE_NETWORK: Byte = 0x01
        @JvmField
        val TYPE_SOCKET: Byte = 0x02
        @JvmField
        val TYPE_HTTP_CODE: Byte = 0x03
        @JvmField
        val TYPE_HTTP_ERROR: Byte = 0x04
        @JvmField
        val TYPE_XML: Byte = 0x05
        @JvmField
        val TYPE_IO: Byte = 0x06
        @JvmField
        val TYPE_RUN: Byte = 0x07

        private const val serialVersionUID = 6243307165131877535L
        private const val Debug = true

        @JvmStatic
        fun http(code: Int): AppException = AppException(TYPE_HTTP_CODE, code, null)

        @JvmStatic
        fun http(e: Exception): AppException = AppException(TYPE_HTTP_ERROR, 0, e)

        @JvmStatic
        fun socket(e: Exception): AppException = AppException(TYPE_SOCKET, 0, e)

        @JvmStatic
        fun io(e: Exception): AppException {
            return when {
                e is UnknownHostException || e is ConnectException -> AppException(TYPE_NETWORK, 0, e)
                e is IOException -> AppException(TYPE_IO, 0, e)
                else -> run(e)
            }
        }

        @JvmStatic
        fun xml(e: Exception): AppException = AppException(TYPE_XML, 0, e)

        @JvmStatic
        fun network(e: Exception): AppException {
            return when {
                e is UnknownHostException || e is ConnectException -> AppException(TYPE_NETWORK, 0, e)
                e is HttpException -> http(e)
                e is SocketException -> socket(e)
                else -> http(e)
            }
        }

        @JvmStatic
        fun run(e: Exception): AppException = AppException(TYPE_RUN, 0, e)

        @JvmStatic
        fun getAppExceptionHandler(): AppException = AppException()

        @JvmStatic
        fun getTraceInfo(a: Context, e: Throwable): StringBuffer {
            val sb = StringBuffer()
            val ex = e.cause ?: e
            val stacks = ex.stackTrace
            for (stack in stacks) {
                sb.append("class: ").append(stack.className).append("; method: ")
                    .append(stack.methodName).append("; line: ").append(stack.lineNumber)
                    .append(";  Exception: ").append(ex.toString()).append("\n")
            }
            return sb
        }
    }
}
