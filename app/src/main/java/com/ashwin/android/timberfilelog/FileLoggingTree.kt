package com.ashwin.android.timberfilelog

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.html.HTMLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.html.CssBuilder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.core.util.FileSize.MB_COEFFICIENT
import ch.qos.logback.core.util.StatusPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import timber.log.Timber
import java.io.File

private val LOG_PREFIX = "logs-" + Build.MODEL
val LATEST_LOG_ADDRESS = "$LOG_PREFIX-latest.html"

class FileLoggingTree(context: Context) : Timber.Tree() {
    private val mLogger: Logger = LoggerFactory.getLogger(FileLoggingTree::class.java)

    init {
        val logDirectory: String = getLogsDir(context).absolutePath
        configureLogger(logDirectory)
    }

    private fun configureLogger(logDirectory: String) {
        // Reset the default context (which may already have been initialized)
        // Since we want to reconfigure it
        val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.reset()
        val rollingFileAppender: RollingFileAppender<ILoggingEvent> = RollingFileAppender()
        rollingFileAppender.context = loggerContext
        rollingFileAppender.isAppend = true
        rollingFileAppender.file = "$logDirectory/$LATEST_LOG_ADDRESS"

        val fileNamingPolicy: SizeAndTimeBasedFNATP<ILoggingEvent> = SizeAndTimeBasedFNATP()
        fileNamingPolicy.context = loggerContext
        fileNamingPolicy.setMaxFileSize(FileSize(5 * MB_COEFFICIENT))

        val rollingPolicy: TimeBasedRollingPolicy<ILoggingEvent> = TimeBasedRollingPolicy()
        rollingPolicy.context = loggerContext
        rollingPolicy.fileNamePattern = "$logDirectory/$LOG_PREFIX.%d{yyyy-MM-dd}.%i.html"
        rollingPolicy.maxHistory = 1
        rollingPolicy.timeBasedFileNamingAndTriggeringPolicy = fileNamingPolicy
        rollingPolicy.setParent(rollingFileAppender) // parent and context required!
        rollingPolicy.start()

        val cssBuilder = CssBuilder { sbuf ->
            sbuf.append(
                """
                            <style type="text/css">
                            table { margin-left: 2em; margin-right: 2em; border-left: 2px solid #AAA; }
                            TR.even { background: #FFFFFF; }
                            TR.odd { background: #EAEAEA; }
                            TR.warn TD.Level, TR.error TD.Level, TR.fatal TD.Level {font-weight: bold; color: #FF4040 }
                            TD { padding-right: 1ex; padding-left: 1ex; border-right: 2px solid #AAA; max-width: 1200px; word-wrap: break-word; }
                            TD.Time, TD.Date { text-align: right; font-family: courier, monospace; font-size: smaller; word-wrap: normal; }
                            TD.Thread { text-align: left; }
                            TD.Level { text-align: right; }
                            TD.Logger { text-align: left; }
                            TR.header { background: #596ED5; color: #FFF; font-weight: bold; font-size: larger; }
                            TD.Exception { background: #A2AEE8; font-family: courier, monospace;}
                            </style>
                            """.trimIndent()
            )
        }

        val htmlLayout = HTMLLayout()
        htmlLayout.context = loggerContext
        htmlLayout.cssBuilder = cssBuilder
        htmlLayout.pattern = "%d{HH:mm:ss.SSS}%level%msg"
        htmlLayout.start()

        val encoder: LayoutWrappingEncoder<ILoggingEvent> = LayoutWrappingEncoder()
        encoder.context = loggerContext
        encoder.layout = htmlLayout
        encoder.start()

        rollingFileAppender.rollingPolicy = rollingPolicy
        rollingFileAppender.encoder = encoder
        rollingFileAppender.start()

        // Add the newly created appenders to the root logger;
        // Qualify Logger to disambiguate from org.slf4j.Logger
        val root: ch.qos.logback.classic.Logger = LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        root.level = Level.TRACE
        root.addAppender(rollingFileAppender)

        // Print any status messages (warnings, etc) encountered in logback config
        StatusPrinter.print(loggerContext)
    }

    private fun logToFile(priority: Int, tag: String, message: String) {
        val logMessage = "$tag: $message"
        when (priority) {
            // No need to track debug logs in support file
            Log.DEBUG -> mLogger.debug(logMessage)
            Log.INFO -> mLogger.info(logMessage)
            Log.WARN -> mLogger.warn(logMessage)
            Log.ERROR -> mLogger.error(logMessage)
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        GlobalScope.launch(Dispatchers.IO) {
            logToFile(priority, tag ?: "", message)
        }
    }
}

fun getLogsDir(context: Context): File {
    val state = Environment.getExternalStorageState()
    val storageDir = if (Environment.MEDIA_MOUNTED == state) {
        // SD card (or partition) available
        context.getExternalFilesDir(null)
    } else {
        // Try internal storage
        context.filesDir
    }
    return File(storageDir, "/logs")
}

fun getLatestLogFile(context: Context): File? {
    return File(
        getLogsDir(context),
        "/$LATEST_LOG_ADDRESS"
    )
}
