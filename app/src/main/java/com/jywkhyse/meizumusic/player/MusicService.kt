// 文件: /app/src/main/java/com/jywkhyse/meizumusic/player/MusicService.kt

package com.jywkhyse.meizumusic.player

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer // 通过Hilt注入ExoPlayer

    private var mediaSession: MediaSession? = null


    // 在服务创建时被调用
    override fun onCreate() {
        super.onCreate()
        // 创建媒体会话，并将ExoPlayer实例关联起来
        mediaSession = MediaSession.Builder(this, player).build()
    }

    // 将媒体会话暴露给其他组件（如UI控制器）
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // --- 添加这个方法 ---
    // 当任务被移除时（例如，用户从最近任务列表中划掉应用）
    override fun onTaskRemoved(rootIntent: Intent?) {
        // 如果播放器没有准备好播放（即处于停止或空闲状态），则可以安全停止服务。
        if (!player.playWhenReady) {
            stopSelf()
        }
        // 如果正在播放或暂停中，则不执行任何操作，让服务继续在后台运行。
    }

    // 在服务销毁时，确保释放资源
    override fun onDestroy() {
        mediaSession?.run {
            player.release() // 释放播放器资源
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}