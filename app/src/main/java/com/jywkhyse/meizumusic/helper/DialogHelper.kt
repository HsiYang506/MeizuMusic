package com.jywkhyse.meizumusic.helper

import android.annotation.SuppressLint
import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input

object DialogHelper {


    // 显示创建歌单的输入框（这个方法你可能已经有了）
    @SuppressLint("CheckResult")
    fun showCreatePlaylistDialog(context: Context, onCreated: (String) -> Unit) {
        MaterialDialog(context).show {
            title(text = "新建歌单")
            input(
                hint = "请输入歌单名称",
                allowEmpty = false // 不允许输入为空
            ) { dialog, text ->
                // 当用户点击“确定”并且输入不为空时，这里的代码会被调用
                val playlistName = text.toString()
                // 调用 ViewModel 创建歌单
                onCreated(playlistName)
            }
            positiveButton(text = "确定")
            negativeButton(text = "取消")
        }
    }

}