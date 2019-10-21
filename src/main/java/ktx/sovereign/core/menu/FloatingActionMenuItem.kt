package ktx.sovereign.core.menu

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.floating_action_menu_item.view.*
import ktx.sovereign.core.R

class FloatingActionMenuItem @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs){
    lateinit var menuItem: MenuItem
    var isTitleEnabled: Boolean = true
        set(value) {
            title_card.visibility = if (value) View.VISIBLE else View.INVISIBLE
            field = value
        }

    init {
        inflate(context, R.layout.floating_action_menu_item, this)
        layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.END }
        orientation = HORIZONTAL
        clipChildren = false
        clipToPadding = false

        val a = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionMenuItem, 0, 0)
        a.style(context)
        a.recycle()
    }

    fun setImageDrawable(drawable: Drawable?) = fab_menu_item.setImageDrawable(drawable)
    fun setTitle(sequence: CharSequence?) {
        isTitleEnabled = sequence.isNullOrEmpty().not()
        title.text = sequence
    }
    override fun setContentDescription(sequence: CharSequence?) {
        fab_menu_item.contentDescription = sequence
    }
    fun setColor(color: Int) = Unit // fab_menu_item.setColorFilter(color)
    fun toggle(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        title_card.visibility = visibility
        fab_menu_item.visibility = visibility
    }
    fun setOnOptionsItemSelectedListener(l: (MenuItem) -> Unit) {
        fab_menu_item.setOnClickListener { l.invoke(menuItem) }
        title_card.setOnClickListener { l.invoke(menuItem) }
    }
    fun enlarge(delay: Long = 0L): Runnable {
        postDelayed({
            with (fab_menu_item) {
                val anim = AnimationUtils.loadAnimation(context, R.anim.enlarge)
                visibility = View.VISIBLE
                startAnimation(anim)
            }
        }, delay)
        return Runnable {
            with (title_card) {
                val anim = AnimationUtils.loadAnimation(context, R.anim.fade_and_translate)
                visibility = View.VISIBLE
                startAnimation(anim)
            }
        }
    }
    fun shrink() {
        val anim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out).apply {
            duration = 130
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) = Unit
                override fun onAnimationRepeat(animation: Animation?) = Unit
                override fun onAnimationEnd(animation: Animation?) {
                    toggle(false)
                    visibility = View.GONE
                }
            })
        }
        startAnimation(anim)
    }
    private fun TypedArray.style(context: Context) {

    }
}