package io.github.toyota32k.networkmonitor

import android.content.Context
import android.net.Uri
import io.github.toyota32k.utils.IUtExternalLogger
import java.io.Closeable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogFile(context: Context): IUtExternalLogger, Closeable {
    private val applicationContext = context.applicationContext
    private var stream = context.openFileOutput("log.txt", Context.MODE_PRIVATE or Context.MODE_APPEND).bufferedWriter(Charsets.UTF_8)
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    val date :String
        get() = LocalDateTime.now().format(dateFormat)
    companion object {
        const val FILENAME = "log.txt"
    }

    init {
        stream.append("Start logging on $date").appendLine()
        stream.flush()
    }

    fun clearAll() {
        stream.flush()
        stream.close()
        stream = applicationContext.openFileOutput(FILENAME, Context.MODE_PRIVATE).bufferedWriter(Charsets.UTF_8)
        stream.append("Start logging on $date").appendLine()
        stream.flush()
    }

    private fun print(level:String, msg:String) {
        stream
            .append("$level: $date: ")
            .append(msg)
            .append('\n')
            .flush()
    }

    override fun debug(msg: String) {
        print("D", msg)
    }

    override fun error(msg: String) {
        print("E", msg)
    }

    override fun info(msg: String) {
        print("I", msg)
    }

    override fun verbose(msg: String) {
        print("V", msg)
    }

    override fun warn(msg: String) {
        print("W", msg)
    }

    override fun close() {
        stream.close()
    }

    fun export(dstUri: Uri) {
        applicationContext.contentResolver.openOutputStream(dstUri)?.use {outStream->
            applicationContext.openFileInput(FILENAME)?.use { inStream->
                inStream.copyTo(outStream)
                outStream.flush()
            }
        }
    }
}