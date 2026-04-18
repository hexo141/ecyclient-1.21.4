package com.example.util

import com.example.splash.SplashVideoPlayer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender

object GameLogInterceptor {
    fun init() {
        val rootLogger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
        
        val appender = object : AbstractAppender("SplashVideoLog", null, null) {
            override fun append(event: LogEvent) {
                val message = event.message.formattedMessage
                if (message.isNotEmpty()) {
                    SplashVideoPlayer.updateLog(message.take(80))
                }
            }
        }
        
        appender.start()
        rootLogger.addAppender(appender)
    }
}