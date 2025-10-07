package com.jywkhyse.meizumusic.popwindow

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.jywkhyse.meizumusic.R
import kotlin.math.min

data class PopupItem(
    val text: String,
    @DrawableRes val icon: Int? = null
)

class PopupWindowList(private val context: Context) {
    private var popupWindow: PopupWindow? = null
    private var anchorView: View? = null
    private var itemData: List<PopupItem>? = null
    private var popAnimStyle: Int = 0
    private var popupWindowWidth: Int = 0
    private var popupWindowHeight: Int = 0
    private var itemClickListener: AdapterView.OnItemClickListener? = null
    private var isModal: Boolean =
        false // isModal is kept for user's API, but we'll ensure focusability
    private var listView: ListView? = null
    private var deviceWidth: Int = 0
    private var deviceHeight: Int = 0
    private var cornerRadius: Float = 16f
    private var elevation: Float = 8f

    init {
        requireNotNull(context) { "Context cannot be null" }
        // setDeviceDimensions() is moved to show() to get up-to-date dimensions
    }

    fun setAnchorView(anchor: View) {
        anchorView = anchor
    }

    fun setItemData(items: List<PopupItem>) {
        itemData = items
    }

    fun setPopAnimStyle(animStyle: Int) {
        popAnimStyle = animStyle
    }

    fun setPopupWindowWidth(width: Int) {
        popupWindowWidth = width
    }

    fun setPopupWindowHeight(height: Int) {
        popupWindowHeight = height
    }

    fun setModal(modal: Boolean) {
        isModal = modal
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
    }

    fun setElevation(elevation: Float) {
        this.elevation = elevation
    }

    fun isShowing(): Boolean {
        return popupWindow?.isShowing == true
    }

    fun hide() {
        if (isShowing()) {
            popupWindow?.dismiss()
        }
    }

    fun setOnItemClickListener(listener: AdapterView.OnItemClickListener) {
        itemClickListener = listener
        listView?.onItemClickListener = listener
    }

    fun show() {
        requireNotNull(anchorView) { "Anchor view cannot be null" }
        requireNotNull(itemData) { "ListView data must be provided" }

        // FIX: Dismiss any existing popup before showing a new one.
        hide()

        // FIX: Get current device dimensions every time to handle orientation changes.
        setDeviceDimensions()

        val adapter = createAdapter()
        listView = ListView(context).apply {
            this.adapter = adapter
            // Apply background with rounded corners and shadow
            background = GradientDrawable().apply {
                setColor(getBackgroundColor())
                cornerRadius = this@PopupWindowList.cornerRadius
            }
            // Apply elevation for shadow (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.elevation = this@PopupWindowList.elevation
            }
            isVerticalScrollBarEnabled = false
            divider = null
            itemClickListener?.let { onItemClickListener = it }
        }

        // --- START OF FIXES for Width and Height ---

        // 1. Measure the actual content size.
        val (measuredContentWidth, totalContentHeight) = measureContentWidthAndHeight(
            adapter,
            listView!!
        )

        // 2. Calculate the final popup width.
        // If user hasn't set a width, use the measured width of the widest item.
        val finalPopupWindowWidth = if (popupWindowWidth > 0) {
            popupWindowWidth
        } else {
            // Add some padding to the measured width for better aesthetics.
            measuredContentWidth + 60
        }

        // 3. Calculate the final popup height.
        // If user hasn't set a height, calculate it based on content and screen size.
        val finalPopupWindowHeight = if (popupWindowHeight > 0) {
            popupWindowHeight
        } else {
            // FIX: The list is scrollable if it exceeds 60% of the device height.
            val maxHeight = (deviceHeight * 0.6).toInt()
            min(totalContentHeight, maxHeight)
        }

        // --- END OF FIXES for Width and Height ---

        // Initialize PopupWindow
        popupWindow = PopupWindow(listView, finalPopupWindowWidth, finalPopupWindowHeight).apply {
            if (popAnimStyle != 0) {
                animationStyle = popAnimStyle
            }
            isOutsideTouchable = true
            // FIX: Popup should always be focusable to receive item clicks.
            isFocusable = true
            setBackgroundDrawable(null) // Transparent background for the window itself.
        }

        // Calculate position
        anchorView?.let { anchor ->
            locateView(anchor)?.let { location ->
                val xMiddle = location.left + anchor.width / 2
                val yMiddle = location.top + anchor.height / 2
                // Use the new final dimensions for positioning calculation
                val x = if (xMiddle > deviceWidth / 2) xMiddle - finalPopupWindowWidth else xMiddle
                val y =
                    if (yMiddle > deviceHeight / 2) yMiddle - finalPopupWindowHeight else yMiddle
                popupWindow?.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
            }
        }
    }

    /**
     * Measures the total desired width and height of the list content.
     */
    private fun measureContentWidthAndHeight(
        adapter: ArrayAdapter<PopupItem>,
        parent: ViewGroup
    ): Pair<Int, Int> {
        var maxWidth = 0
        var totalHeight = 0
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        for (i in 0 until adapter.count) {
            val view = adapter.getView(i, null, parent)
            view.measure(spec, spec)
            if (view.measuredWidth > maxWidth) {
                maxWidth = view.measuredWidth
            }
            totalHeight += view.measuredHeight
        }
        return Pair(maxWidth, totalHeight)
    }

    private fun createAdapter(): ArrayAdapter<PopupItem> {
        return object : ArrayAdapter<PopupItem>(context, R.layout.popup_item_layout, itemData!!) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = convertView ?: View.inflate(context, R.layout.popup_item_layout, null)
                val item = getItem(position)
                val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
                val iconView = view.findViewById<android.widget.ImageView>(R.id.icon)

                textView.text = item?.text
                if (item?.icon != null) {
                    iconView.visibility = View.VISIBLE
                    iconView.setImageResource(item.icon)
                } else {
                    iconView.visibility = View.GONE
                }

                // Apply text color based on theme
                textView.setTextColor(getTextColor())
                return view
            }
        }
    }

    private fun getBackgroundColor(): Int {
        return ContextCompat.getColor(
            context,
            if (isDarkMode()) R.color.md_grey_800 else R.color.md_blue_grey_50
        )
    }

    private fun getTextColor(): Int {
        return ContextCompat.getColor(
            context,
            if (isDarkMode()) R.color.md_white_87 else R.color.md_black_87
        )
    }

    private fun isDarkMode(): Boolean {
        return context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun locateView(view: View?): Rect? {
        if (view == null) return null
        val locInt = IntArray(2)
        try {
            view.getLocationOnScreen(locInt)
        } catch (e: NullPointerException) {
            return null
        }
        return Rect().apply {
            left = locInt[0]
            top = locInt[1]
            right = left + view.width
            bottom = top + view.height
        }
    }

    private fun setDeviceDimensions() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outSize = android.graphics.Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = context.display
            display?.getRealSize(outSize)
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getSize(outSize)
        }
        deviceWidth = outSize.x
        deviceHeight = outSize.y
    }
}