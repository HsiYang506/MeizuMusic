package com.jywkhyse.meizumusic.helper

import android.content.Context
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import com.jywkhyse.meizumusic.helper.ThemeColorHelper.getPrimaryColor
import com.jywkhyse.meizumusic.helper.ThemeColorHelper.getTextColorPrimary
import com.jywkhyse.meizumusic.helper.ThemeColorHelper.getTextColorSecondary
import com.jywkhyse.meizumusic.helper.ThemeColorHelper.getWindowBackground

import com.google.android.material.R as MaterialR


object ThemeColorHelper {

    /**
     * A generic function to resolve any theme attribute color.
     * @param attrRes The resource ID of the attribute to resolve (e.g., R.attr.colorPrimary).
     * @return The resolved color integer.
     */
    fun getAttributeColor(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    // Key colors from Material Design theme
    fun getPrimaryColor(context: Context) =
        getAttributeColor(context, MaterialR.attr.colorPrimary)

    fun getOnPrimaryColor(context: Context) =
        getAttributeColor(context, MaterialR.attr.colorOnPrimary)

    fun getSecondaryColor(context: Context) =
        getAttributeColor(context, MaterialR.attr.colorSecondary)

    fun getOnSecondaryColor(context: Context) =
        getAttributeColor(context, MaterialR.attr.colorOnSecondary)

    fun getBackgroundColor(context: Context) =
        getAttributeColor(context, MaterialR.attr.colorSurface)

    fun getOnBackgroundColor(context: Context) =
        getAttributeColor(context, MaterialR.attr.colorOnSurface)

    // Text colors
    fun getTextColorPrimary(context: Context) =
        getAttributeColor(context, android.R.attr.textColorPrimary)

    fun getTextColorSecondary(context: Context) =
        getAttributeColor(context, android.R.attr.textColorSecondary)

    // Window background
    fun getWindowBackground(context: Context) =
        getAttributeColor(context, android.R.attr.windowBackground)

}

// --- View Extension Functions for easy application ---

fun TextView.applyPrimaryTextColor(context: Context) {
    this.setTextColor(getTextColorPrimary(context))
}

fun TextView.applySecondaryTextColor(context: Context) {
    this.setTextColor(getTextColorSecondary(context))
}

fun ImageView.applyPrimaryColorFilter(context: Context) {
    this.setColorFilter(getPrimaryColor(context))
}

fun ImageView.applyTextColorPrimaryFilter(context: Context) {
    this.setColorFilter(getTextColorPrimary(context), PorterDuff.Mode.SRC_ATOP)
}

fun ImageView.applyTextColorSecondaryFilter(context: Context) {
    this.setColorFilter(getTextColorSecondary(context), PorterDuff.Mode.SRC_ATOP)
}

fun View.applyBackgroundColor(context: Context) {
    this.setBackgroundColor(getWindowBackground(context))
}