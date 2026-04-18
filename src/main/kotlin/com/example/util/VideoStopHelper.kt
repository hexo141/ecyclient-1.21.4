package com.example.util

import com.example.splash.SplashVideoPlayer

object VideoStopHelper {
    private var hasStoppedVideo = false
    private var hasStartedVideo = false
    
    fun startVideo() {
        if (!hasStartedVideo) {
            // 在游戏启动的早期播放视频
            println("游戏启动，播放启动视频...")
            SplashVideoPlayer.playVideo()
            hasStartedVideo = true
        }
    }
    
    fun onRenderStart() {
        if (!hasStoppedVideo) {
            // 这是游戏窗口显示后，第一个渲染帧开始的位置
            // 此时窗口已存在，纹理等资源正准备加载
            println("渲染循环开始，窗口已显示，停止启动视频...")
            SplashVideoPlayer.stopVideo()
            hasStoppedVideo = true
        }
    }
}