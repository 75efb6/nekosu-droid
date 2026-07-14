package com.edlplan.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.edlplan.replay.OdrConfig
import com.edlplan.replay.OdrDatabase
import com.edlplan.replay.OsuDroidReplayPack
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ImportReplayActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        if (intent.data != null) {
            val path = intent.data!!.path
            println("path: $path")
            val file = File(path)
            if (!file.exists() || file.isDirectory) {
                Toast.makeText(this, R.string.invalid_edr_file, Toast.LENGTH_SHORT).show()
                super.onStart()
                finish()
                return
            }
            try {
                val entry = OsuDroidReplayPack.unpack(FileInputStream(file))
                val rep = File(OdrConfig.getScoreDir(), entry.replay!!.replayFileName)
                if (!rep.exists()) {
                    if (!rep.createNewFile()) {
                        Toast.makeText(this, R.string.failed_to_import_edr, Toast.LENGTH_SHORT).show()
                        super.onStart()
                        finish()
                        return
                    }
                }
                rep.outputStream().use { outputStream ->
                    outputStream.write(entry.replayFile)
                }
                entry.replay!!.replayFile = rep.absolutePath
                if (OdrDatabase.get().write(entry.replay!!) != -1L) {
                    Toast.makeText(this, R.string.import_edr_successfully, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.failed_to_import_edr, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    String.format(resources.getString(R.string.failed_to_import_edr_with_err), e.toString()),
                    Toast.LENGTH_SHORT
                ).show()
                super.onStart()
                finish()
                return
            }
        }
        super.onStart()
    }
}
