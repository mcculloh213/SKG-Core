package ktx.sovereign.core.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.view.children
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

        @JvmStatic private fun runOnHVExpandDirection(
            direction: ExpandDirection,
            onVertical: (() -> Unit)? = null,
            onHorizontal: (() -> Unit)? = null
        ) = when (direction) {
            ExpandDirection.UP, ExpandDirection.DOWN -> {
                onVertical?.invoke() ?: Unit
            }
            ExpandDirection.START, ExpandDirection.LEFT,
            ExpandDirection.END, ExpandDirection.RIGHT -> {
                onHorizontal?.invoke() ?: Unit
            }
        }
    }
    enum class ExpandDirection {
        UP,             // 0
        DOWN,           // 1
        LEFT,   START,  // 2
        RIGHT,  END,    // 3
    }
    enum class LabelPlacement {
        NONE,           // -1
        TOP,    ABOVE,  // 0
        BOTTOM, BELOW,  // 1
        START,  LEFT,   // 2
        END,    RIGHT   // 3
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
    private val placement: LabelPlacement = LabelPlacement.LEFT
    private val containerRect: Rect = Rect()
    private val childRect: Rect = Rect()

    private val itemSpacing = resources.getDimension(R.dimen.layout_margin_small)
    private val labelMargin = resources.getDimensionPixelSize(R.dimen.layout_margin_medium)
    private val labelVerticalOffset = resources.getDimensionPixelSize(R.dimen.layout_margin_xsmall)

    private var maxWidth: Int = 0
    private var maxHeight: Int = 0
    private var itemCount: Int = 1

    var expanded: Boolean = false
        private set

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
            visibility = View.VISIBLE
        }
        addView(menu, 0, generateDefaultLayoutParams())
        itemCount = childCount
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 0
        var height = 0
        var childState = 0

        maxWidth = 0
        maxHeight = 0
        var maxLabelWidth = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val params = if (checkLayoutParams(child.layoutParams)) {
                    child.layoutParams
                } else {
                    val p = generateLayoutParams(child.layoutParams)
                    child.layoutParams = p
                    p
                }
                when (direction) {
                    ExpandDirection.UP, ExpandDirection.DOWN -> {
                        maxWidth = max(maxWidth, child.measuredWidth)
                        height += child.measuredHeight
                    }
                    ExpandDirection.START, ExpandDirection.LEFT,
                    ExpandDirection.END, ExpandDirection.RIGHT -> {
                        width += child.measuredWidth
                        maxHeight = max(maxHeight, child.measuredHeight)
                    }
                }
                childState = View.combineMeasuredStates(childState, child.measuredState)
            }

            if (!isHorizontal()) {
                width = max(maxWidth, suggestedMinimumWidth)
            } else {
                height = max(maxHeight, suggestedMinimumHeight)
            }

            runOnHVExpandDirection(direction,
                onVertical = {
                    height += (itemSpacing * (itemCount - 1)).toInt()
                    height = adjustForOvershoot(height)
                },
                onHorizontal = {
                    width += (itemSpacing * (itemCount - 1)).toInt()
                    width = adjustForOvershoot(width)
                }
            )

            setMeasuredDimension(
                View.resolveSizeAndState(width, widthMeasureSpec, childState),
                View.resolveSizeAndState(height, heightMeasureSpec,
                    childState shl View.MEASURED_HEIGHT_STATE_SHIFT
                )
            )
        }
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        runOnHVExpandDirection(direction,
            onVertical = {
                val isUp = direction == ExpandDirection.UP

                val menuY = if (isUp) {
                    b - t - menu.measuredHeight
                } else {
                    0
                }
                val centerX = measuredWidth / 2
                val menuLeft = centerX - (menu.measuredWidth / 2)

                menu.layout(menuLeft, menuY, menuLeft + menu.measuredWidth, menuY + menu.measuredHeight)

                var nextY = if (isUp) {
                    menuY - itemSpacing
                } else {
                    menuY + menu.measuredHeight + itemSpacing
                }.toInt()
                for (i in 0 until childCount) {
                    val child = getChildAt(i) ?: continue
                    if (child == menu || child.visibility == GONE) continue
                    val childX = centerX - (child.measuredWidth / 2)
                    val childY = if (isUp) {
                        nextY - child.measuredHeight
                    } else {
                        nextY
                    }
                    child.layout(childX, childY, childX + child.measuredWidth, childY + child.measuredHeight)

                    val collapsedTransition = (menuY - childY).toFloat()
                    val expandedTranslation = 0f

                    child.translationY = if (expanded) {
                        expandedTranslation
                    } else {
                        collapsedTransition
                    }
                    child.alpha = if (expanded) {
                        1f
                    } else {
                        0f
                    }

                    val params = child.layoutParams as LayoutParams
                    params.apply {
                        setFloatValues(expandedTranslation, collapsedTransition)
                        setAnimationsTarget(child)
                    }

                    nextY = if (isUp) {
                        childY - itemSpacing
                    } else {
                        childY + child.measuredHeight + itemSpacing
                    }.toInt()
                }
            })
    }
    override fun onFinishInflate() {
        super.onFinishInflate()
        bringChildToFront(menu)
        requestLayout()
        invalidate()
        itemCount = childCount
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
    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
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
        ExpandDirection.START, ExpandDirection.RIGHT,
        ExpandDirection.END, ExpandDirection.LEFT
    )
    private fun expandsUp(): Boolean = direction === ExpandDirection.UP

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


    inner class LayoutParams(source: ViewGroup.LayoutParams) : MarginLayoutParams(source) {
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
                ExpandDirection.UP, ExpandDirection.DOWN -> {
                    animatorCollapseMenu.setProperty(View.TRANSLATION_Y)
                    animatorExpandMenu.setProperty(View.TRANSLATION_Y)
                }
                ExpandDirection.START, ExpandDirection.LEFT,
                ExpandDirection.END, ExpandDirection.RIGHT -> {
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