package ktx.sovereign.core.menu

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ktx.sovereign.core.R
import java.util.*

@SuppressLint("CustomViewStyleable")
class FloatingActionOptions @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val menu = MenuBuilder(context)
    private val options: MutableList<FloatingActionMenuItem> = LinkedList()
    private val avdOpenToClose: AnimatedVectorDrawableCompat? =
            AnimatedVectorDrawableCompat.create(context, R.drawable.menu_to_close)
    private val avdCloseToOpen: AnimatedVectorDrawableCompat? =
            AnimatedVectorDrawableCompat.create(context, R.drawable.close_to_menu)
    private val toggle: FloatingActionButton
    private lateinit var container: FloatingActionMenuLayout

    var isOpen: Boolean = false
//        set(value) {
//            toggle(value)
//            field = value
//        }

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        val v = inflate(context, R.layout.floating_action_options_toggle, this)
        toggle = v.findViewById(R.id.fab_menu_toggle)

        orientation = VERTICAL
        clipChildren = false

        val a = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionMenuLayout, defStyleAttr, defStyleRes)
        a.styleToggleFab(context)
        a.styleMenuOptions(context)
        a.recycle()
    }

    fun setToggleClickListener(layout: FloatingActionMenuLayout, listener: OnClickListener?) {
        container = layout
        toggle.setOnClickListener { view ->
            postDelayed({
                if (!isOpen) {
                    layout.elevation = px2dp(context, 5f)
                    isOpen = true
                    visibilitySetup(View.VISIBLE)
                } else {
                    listener?.onClick(view)
                }
            }, 50)
        }
    }
    fun setOptionsItemSelectedListener(l: (MenuItem) -> Unit) {
        options.forEach { item -> item.setOnOptionsItemSelectedListener(l) }
    }
    fun openMenu() {
        isOpen = true
        visibilitySetup(View.VISIBLE)
    }
    fun closeMenu() {
        isOpen = false
        visibilitySetup(View.GONE)
    }

    private fun toggle(open: Boolean) {
        options.forEach { item ->
            item.toggle(open)
        }
    }
    private fun TypedArray.styleToggleFab(context: Context) {
        val color = getColor(R.styleable.FloatingActionMenuLayout_toggleColor, R.attr.colorAccent)
        toggle.apply {
            setBackgroundColor(color)
            setImageDrawable(avdOpenToClose)
        }
    }
    private fun TypedArray.styleMenuOptions(context: Context) {
        val inflater = MenuInflater(context)
        val color = getColor(R.styleable.FloatingActionMenuLayout_optionsColor, R.attr.colorSecondary)
        val menuRes = getResourceId(R.styleable.FloatingActionMenuLayout_optionsMenu, -1)
        if (menuRes != -1) {
            inflater.inflate(menuRes, menu)
            menu.forEach { item ->
                val fab = FloatingActionMenuItem(context).apply {
                    setTitle(item.title)
                    setImageDrawable(item.icon)
                    setColor(color)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        contentDescription = item.contentDescription
                    }
                    menuItem = item
                }
                addView(fab)
                options.add(fab)
            }
        }
        toggle(isOpen)
    }
    private fun visibilitySetup(visible: Int) = when (visible) {
        View.VISIBLE -> {
            options.forEachIndexed { i, item ->
                item.visibility = View.VISIBLE
                val runnable = item.enlarge(i * 15L)
                if (item.isTitleEnabled) {
                    postDelayed(runnable, options.size * 18L)
                }
            }
        }
        View.INVISIBLE -> Unit
        View.GONE -> {
            options.forEach { item -> item.shrink()}
        }
        else -> throw IllegalArgumentException("Unknown visibility modifier: $visible")
    }
}
fun px2dp(context: Context, pixel: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, pixel, context.resources.displayMetrics
)