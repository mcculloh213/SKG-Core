package ktx.sovereign.core.menu

import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.widget.FrameLayout
import ktx.sovereign.core.R

class FloatingActionMenuLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    val isOpen: Boolean
        get() = options.isOpen
    private val options = FloatingActionOptions(context, attrs, defStyleAttr, defStyleRes)
    private var rootColor: Int = 0

    init {
        applyStyle(context, attrs, defStyleAttr, defStyleRes)
    }
    fun open() {

    }
    fun close() {
        options.closeMenu()
    }

    fun setToggleClickListener(l: OnClickListener?) {
        options.setToggleClickListener(this, l)
    }
    fun setOnFloatingActionOptionItemSelectedListener(l: (MenuItem) -> Unit) {
        options.setOptionsItemSelectedListener(l)
    }
    private fun applyStyle(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.FloatingActionMenuLayout, defStyleAttr, defStyleRes
        )
        rootColor = a.getColor(R.styleable.FloatingActionMenuLayout_rootBackgroundColor, R.attr.colorSurface)

        addView(options)
        a.recycle()
    }
}