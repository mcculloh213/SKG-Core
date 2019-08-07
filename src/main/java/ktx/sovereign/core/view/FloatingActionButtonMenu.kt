package ktx.sovereign.core.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ktx.sovereign.core.R
import kotlin.math.max

class FloatingActionButtonMenu @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.floatingActionButtonStyle
) : ViewGroup(context, attrs, defStyleAttr) {
    companion object {
        private val ExpandInterpolator: Interpolator = OvershootInterpolator()
        private val CollapseInterpolator: Interpolator = DecelerateInterpolator(3f)
        private val AlphaExpandInterpolator: Interpolator = DecelerateInterpolator()
    }
    enum class ExpandDirection {
        NORTH, UP,      // 0
        EAST, RIGHT,    // 1
        SOUTH, DOWN,    // 2
        WEST, LEFT      // 3
    }
    enum class LabelPlacement {
        NONE,           // -1
        TOP, ABOVE,     // 0
        END, TO_RIGHT,  // 1
        BOTTOM, BELOW,  // 2
        START, TO_LEFT  // 3
    }

    private val avdMenuToClose: AnimatedVectorDrawableCompat? =
        AnimatedVectorDrawableCompat.create(context, R.drawable.menu_to_close)
    private val avdCloseToMenu: AnimatedVectorDrawableCompat? =
        AnimatedVectorDrawableCompat.create(context, R.drawable.close_to_menu)
    private val animationExpand: AnimatorSet = AnimatorSet().also { it.duration = 300 }
    private val animationCollapse: AnimatorSet = AnimatorSet().also { it.duration = 300 }
    private val menu: FloatingActionButton = FloatingActionButton(context, attrs, defStyleAttr)
    private val enableLabels: Boolean = true
    private val direction: ExpandDirection = ExpandDirection.DOWN
    private val placement: LabelPlacement = LabelPlacement.TO_LEFT

    private val itemSpacing = resources.getDimension(R.dimen.layout_margin_small)
    private val labelMargin = resources.getDimensionPixelSize(R.dimen.layout_margin_medium)
    private val labelVerticalOffset = resources.getDimensionPixelSize(R.dimen.layout_margin_xsmall)


    private var maxWidth: Int = 0
    private var maxHeight: Int = 0
    private var itemCount: Int = 1

    var expanded: Boolean = false
        private set(value) {
            field = value
        }

    init {
        menu.apply {
            id = R.id.fab_menu
            size = FloatingActionButton.SIZE_MINI
            contentDescription = "hf_no_number|hf_show_text|Toggle Menu"
            setImageDrawable(avdMenuToClose)
            setOnClickListener {
                val avd = if (expanded) {
                    avdCloseToMenu
                } else {
                    avdMenuToClose
                }
                setImageDrawable(avd)
                avd?.start()
                toggle()
            }
            setTag(R.id.metadata_label, "Menu")
        }
        addView(menu, generateDefaultLayoutParams())
    }

    fun addButton(@IdRes idRes: Int, @DrawableRes iconRes: Int, label: String, onClick: (() -> Unit)? = null) {
        val add = FloatingActionButton(context).apply {
            id = idRes
            size = FloatingActionButton.SIZE_MINI
            contentDescription = "hf_no_number|hf_show_text|$label"
            setImageResource(iconRes)
            layoutParams = generateDefaultLayoutParams()
            setTag(R.id.metadata_label, label)
            setOnClickListener { onClick?.invoke() }
        }
        addView(add, itemCount - 1)
        itemCount++

//        if (enableLabels) {
//            createLabels()
//        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        var width = 0
        var height = 0

        maxWidth = 0
        maxHeight = 0
        var maxLabelWidth = 0

        for (i in 0..itemCount) {
            val child = getChildAt(i)
            if (child == null || child.visibility == GONE) continue
            when (direction) {
                ExpandDirection.NORTH, ExpandDirection.SOUTH,
                ExpandDirection.UP, ExpandDirection.DOWN -> {
                    maxWidth = max(maxWidth, child.measuredWidth)
                    height += child.measuredHeight
                }
                ExpandDirection.EAST, ExpandDirection.WEST,
                ExpandDirection.RIGHT, ExpandDirection.LEFT -> {
                    width += child.measuredWidth
                    maxHeight = max(maxHeight, child.measuredHeight)
                }
            }

            if (!isHorizontal()) {
                val label = child.getTag(R.id.fab_menu_label) as TextView?
                if (label != null) {
                    maxLabelWidth = max(maxLabelWidth, label.measuredWidth)
                }
            }
        }

        if (!isHorizontal()) {
            width = maxWidth + (if (maxLabelWidth > 0) (maxLabelWidth + labelMargin) else 0)
        } else {
            height = maxHeight
        }

        when (direction) {
            ExpandDirection.NORTH, ExpandDirection.SOUTH,
            ExpandDirection.UP, ExpandDirection.DOWN -> {
                height += (itemSpacing * (itemCount - 1)).toInt()
                height = adjustForOvershoot(height)
            }
            ExpandDirection.EAST, ExpandDirection.WEST,
            ExpandDirection.RIGHT, ExpandDirection.LEFT -> {
                width += (itemSpacing * (itemCount - 1)).toInt()
                width = adjustForOvershoot(width)
            }
        }

        setMeasuredDimension(width, height)
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        when (direction) {
            ExpandDirection.NORTH, ExpandDirection.SOUTH,
            ExpandDirection.UP, ExpandDirection.DOWN -> {
                val isUp = direction in arrayOf(ExpandDirection.NORTH, ExpandDirection.UP)
                val posLeft = placement in arrayOf(LabelPlacement.START, LabelPlacement.TO_LEFT)

                val menuY = if (isUp) (b - t - menu.measuredHeight) else 0
                val horizCenter = if (posLeft) (r - 1 - (maxWidth / 2)) else (maxWidth / 2)
                val menuLeft = horizCenter - (menu.measuredWidth / 2)

                menu.layout(menuLeft, menuY, menuLeft + menu.measuredWidth, menuY + menu.measuredHeight)

                val labelOffset = (maxWidth / 2) + labelMargin
                val labelX = if (posLeft) {
                    horizCenter - labelOffset
                } else {
                    horizCenter + labelOffset
                }
                var nextY = if (isUp) {
                    menuY - itemSpacing
                } else {
                    menuY + menu.measuredHeight + itemSpacing
                }.toInt()

                for (i in 0..itemCount) { // (itemCount - 1) downTo 0) {
                    val child = getChildAt(i)
                    if (child == null || child == menu || child.visibility == GONE) continue
                    val childX = horizCenter - (child.measuredWidth / 2)
                    val childY = if (isUp) (nextY - child.measuredHeight) else nextY
                    child.layout(childX, childY, childX + child.measuredWidth, childY + child.measuredHeight)

                    val collapsedTranslation = (menuY - childY).toFloat()
                    val expandedTranslation = 0f

                    child.translationY = if (expanded) expandedTranslation else collapsedTranslation
                    child.alpha = if (expanded) 1f else 0f

                    val params = child.layoutParams as LayoutParams
                    params.apply {
                        setFloatValues(expandedTranslation, collapsedTranslation)
                        setAnimationsTarget(child)
                    }
                    (child.getTag(R.id.fab_menu_label) as View?)?.let {
                        val awayX = if (posLeft) (labelX - it.measuredWidth) else labelX + it.measuredWidth
                        val left = if (posLeft) awayX else labelX
                        val right = if (posLeft) labelX else  awayX
                        val top = childY - labelVerticalOffset + ((child.measuredHeight - it.measuredHeight) / 2)

                        it.layout(left, top, right, top + it.measuredHeight)
                        it.translationY = if (expanded) expandedTranslation else collapsedTranslation
                        it.alpha = if (expanded) 1f else 0f
                        val labelParams = it.layoutParams as LayoutParams
                        labelParams.setFloatValues(expandedTranslation, collapsedTranslation)
                        labelParams.setAnimationsTarget(it)
                    }
                    nextY = if (isUp) {
                        childY - itemSpacing
                    } else {
                        childY + child.measuredHeight + itemSpacing
                    }.toInt()
                }
            }
            ExpandDirection.EAST, ExpandDirection.WEST,
            ExpandDirection.RIGHT, ExpandDirection.LEFT -> {

            }
        }
    }
    override fun onFinishInflate() {
        super.onFinishInflate()
        bringChildToFront(menu)
        itemCount = childCount

//        if (enableLabels) {
//            createLabels()
//        }
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(super.generateDefaultLayoutParams())
    }
    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(super.generateLayoutParams(attrs))
    }
    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(super.generateLayoutParams(p))
    }

    fun toggle() {
        if (expanded) {
            collapse()
        } else {
            expand()
        }
    }
    fun collapse(immediate: Boolean = false) {
        if (expanded) {
            expanded = false
            animationCollapse.duration = if (immediate) 0L else 300L
            animationExpand.cancel()
            animationCollapse.start()
        }
    }
    fun expand() {
        if (!expanded) {
            expanded = true
            animationCollapse.cancel()
            animationExpand.start()
        }
    }

    private fun adjustForOvershoot(dimension: Int) = dimension * 12 / 10
    private fun isHorizontal(): Boolean = direction in arrayOf(
        ExpandDirection.EAST, ExpandDirection.RIGHT,
        ExpandDirection.WEST, ExpandDirection.LEFT
    )
    private fun expandsUp(): Boolean = direction in arrayOf(
        ExpandDirection.NORTH, ExpandDirection.UP
    )

    private fun createLabels() {
        val background = TypedValue()
        val textAppearance = TypedValue()
        val textColor = TypedValue()
        with (context.theme) {
            resolveAttribute(R.attr.colorSecondary, background, true)
            resolveAttribute(android.R.attr.textAppearanceSmall, textAppearance, true)
            resolveAttribute(android.R.attr.textColorPrimary, textColor, true)
        }
        for (i in 0..itemCount) {
            val item = getChildAt(i)
            if (item == null || item == menu || item.getTag(R.id.fab_menu_label) != null) continue
            val label = TextView(context).apply {
                text = item.getTag(R.id.metadata_label)
                setBackgroundColor(background.data)
                setTextAppearance(textAppearance.data)
                setTextColor(textColor.data)
            }
            addView(label)
            item.setTag(R.id.fab_menu_label, label)
        }
    }

    inner class LayoutParams(source: ViewGroup.LayoutParams) : ViewGroup.LayoutParams(source) {
        private val animatorExpandMenu: ObjectAnimator = ObjectAnimator()
        private val animatorExpandAlpha: ObjectAnimator = ObjectAnimator()
        private val animatorCollapseMenu: ObjectAnimator = ObjectAnimator()
        private val animatorCollapseAlpha: ObjectAnimator = ObjectAnimator()
        private var ready = false

        init {
            animatorExpandMenu.interpolator = ExpandInterpolator
            animatorExpandAlpha.interpolator = AlphaExpandInterpolator
            animatorCollapseMenu.interpolator = CollapseInterpolator
            animatorCollapseAlpha.interpolator = CollapseInterpolator

            animatorCollapseAlpha.apply {
                setProperty(View.ALPHA)
                setFloatValues(1f, 0f)
            }
            animatorExpandAlpha.apply {
                setProperty(View.ALPHA)
                setFloatValues(0f, 1f)
            }

            when (direction) {
                ExpandDirection.NORTH, ExpandDirection.SOUTH,
                ExpandDirection.UP, ExpandDirection.DOWN -> {
                    animatorCollapseMenu.setProperty(View.TRANSLATION_Y)
                    animatorExpandMenu.setProperty(View.TRANSLATION_Y)
                }
                ExpandDirection.EAST, ExpandDirection.WEST,
                ExpandDirection.RIGHT, ExpandDirection.LEFT -> {
                    animatorCollapseMenu.setProperty(View.TRANSLATION_X)
                    animatorExpandMenu.setProperty(View.TRANSLATION_X)
                }
            }
        }

        fun setFloatValues(expanded: Float, collapsed: Float) {
            animatorCollapseMenu.setFloatValues(expanded, collapsed)
            animatorExpandMenu.setFloatValues(collapsed, expanded)
        }

        fun setAnimationsTarget(view: View) {
            animatorCollapseAlpha.target = view
            animatorCollapseMenu.target = view
            animatorExpandAlpha.target = view
            animatorExpandMenu.target = view

            if (!ready) {
                addLayerTypeListener(animatorExpandMenu, view)
                addLayerTypeListener(animatorCollapseMenu, view)
                animationCollapse.playTogether(animatorCollapseAlpha, animatorCollapseMenu)
                animationExpand.playTogether(animatorExpandAlpha, animatorExpandMenu)
                ready = true
            }
        }

        private fun addLayerTypeListener(animator: Animator, view: View) {
            animator.addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                }
                override fun onAnimationStart(animation: Animator?) {
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }
            })
        }
    }
}