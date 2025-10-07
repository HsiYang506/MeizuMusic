package com.jywkhyse.meizumusic.helper

import android.annotation.SuppressLint
import android.view.View
import com.jywkhyse.meizumusic.R
import com.jywkhyse.meizumusic.popwindow.PopupItem
import com.jywkhyse.meizumusic.popwindow.PopupWindowList


object PopMenuHelper {
    val ALL_OPTION = listOf(
        "添加到歌单..." to R.drawable.ic_playlist_add,
        "下一首播放" to R.drawable.ic_next_play,
        "分享" to R.drawable.ic_share,
        "歌曲信息" to R.drawable.ic_info,
        "永久删除" to R.drawable.ic_delete_forever
    )
    val PLAY_LIST = listOf(
        "重命名" to R.drawable.ic_edit,
        "删除" to R.drawable.ic_delete_forever
    )


    @SuppressLint("StaticFieldLeak")
    private var mPopupWindowList: PopupWindowList? = null

    sealed class PopMenuType()
    object All : PopMenuType()
    object PlayList : PopMenuType()
    object PlayListDetails : PopMenuType()

    fun showOptionsMenu(
        anchorView: View,
        type: PopMenuType = All,
        call: (Int, String) -> Unit
    ) {
        val context = anchorView.context
        val list = when (type) {
            is PlayList -> PLAY_LIST
            else -> ALL_OPTION
        }.map { PopupItem(it.first, it.second) }
        if (mPopupWindowList == null) {
            mPopupWindowList = PopupWindowList(context)
        }
        mPopupWindowList?.setAnchorView(anchorView)
        mPopupWindowList?.setItemData(list)
        mPopupWindowList?.setOnItemClickListener { parent, view, position, id ->
            mPopupWindowList?.hide()
            call(position, list[position].text)
        }
        mPopupWindowList?.setModal(true)
        mPopupWindowList?.show()
    }

}
