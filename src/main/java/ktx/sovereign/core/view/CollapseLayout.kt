package ktx.sovereign.core.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import ktx.sovereign.core.R
import ktx.sovereign.core.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt

class CollapseLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    val expanded: Boolean
        get() = _expanded.get()
    val maxWidth: Int
        get() = _maxWidth
    val maxHeight: Int
        get() = _maxHeight

    private val _expanded: AtomicBoolean = AtomicBoolean(true)
//    private val _spacing = resources.getDimension(R.dimen.layout_margin_small)
    private val _layoutRect: Rect = Rect()
    private val _paddingRect: Rect = Rect()
    private val _containerRect: Rect = Rect()
    private val _toggleRect: Rect = Rect()
    private val _childRect: Rect = Rect()
    private val expandAnimation: AnimatorSet = AnimatorSet().also { it.duration = 300 }
    private val collapseAnimation: AnimatorSet = AnimatorSet().also { it.duration = 300 }
    private val directive: String
        get() = if (expanded) {
            context.getString(R.string.hf_hide_menu)
        } else {
            context.getString(R.string.hf_show_menu)
        }

    private var _maxWidth: Int = 0
    private var _maxHeight: Int = 0
    private var toggleStateListener: OnToggleStateChanged? = null
    private lateinit var _toggle: View

    private val autoCollapseTimer: Timer = Timer(5_000) { collapse() }

    fun setToggleView(view: View) {
        _toggle = view
        _toggle.apply {
            contentDescription = directive
            setTag(R.id.collapse_container_toggle, context.getString(R.string.collapse_layout_toggle))
            setOnClickListener { toggle() }
        }
    }
    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    fun <T : View> getToggleView(): T = if (::_toggle.isInitialized) {
        try {
            _toggle as T
        } catch (ex: ClassCastException) {
            throw ex
        }
    } else {
        findToggleView<T>().also {
            _toggle = it.apply {
                setOnClickListener { toggle() }
            }
        }
    }
    fun <T : View> findToggleView(): T = with(context.getString(R.string.collapse_layout_toggle)) {
        findViewWithTag(this) ?: throw RuntimeException("No view with tag '$this' was found.")
    }
    fun setOnToggleStateChangedListener(listener: OnToggleStateChanged?) {
        toggleStateListener = listener
    }
    @Synchronized
    fun toggle(): Boolean = if (!::_toggle.isInitialized) {
        false
    } else {
        Log.i("CollapseLayout", "Toggle: Expanded? -- $expanded")
        if (expanded) {
            collapse()
        } else {
            expand()
        }
    }
    fun expand(): Boolean = if (expanded) {
        false
    } else {
        synchronized(this@CollapseLayout) {
            _expanded.set(true)
            collapseAnimation.cancel()
            expandAnimation.start()
            autoCollapseTimer.begin()
            toggleStateListener?.onStateChanged()
            true
        }
    }
    @JvmOverloads
    fun collapse(immediate: Boolean = false): Boolean = if (expanded) {
        synchronized(this@CollapseLayout) {
            _expanded.set(false)
            expandAnimation.cancel()
            collapseAnimation.duration = if (immediate) 0L else 300L
            collapseAnimation.start()
            if (autoCollapseTimer.running) {
                autoCollapseTimer.end()
            }
            toggleStateListener?.onStateChanged()
            true
        }
    } else {
        false
    }

    fun resetTimer() = autoCollapseTimer.reset()

    override fun shouldDelayChildPressedState(): Boolean = false
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        _maxWidth = 0
        _maxHeight = 0

        var width = 0
        var height = 0
        var childState = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val p = child.layoutParams as CollapseLayoutParams
                width = max(width, child.measuredWidth + p.leftMargin + p.rightMargin)
                height += child.measuredHeight + p.topMargin + p.bottomMargin // + _spacing.toInt()
                childState = View.combineMeasuredStates(childState, child.measuredState)
            }
        }

        width = max(width, suggestedMinimumWidth)
        _maxWidth = max(width, suggestedMinimumWidth)
        height = max(height, suggestedMinimumHeight)
        _maxHeight = adjustForOvershoot(height)

        setMeasuredDimension(
                View.resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                View.resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState shl View.MEASURED_HEIGHT_STATE_SHIFT)
        )
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        with (_layoutRect) {
            left = l
            top = t
            right = r
            bottom = b
        }
        with (_paddingRect) {
            left = paddingLeft
            right = r - l - paddingRight
            top = paddingTop
            bottom = b - t - paddingBottom
        }

        val toggle = if (::_toggle.isInitialized) { _toggle } else { getToggleView() }
        layoutToggleView(toggle)

        for (i in 0 until count) {
            val child = getChildAt(i) ?: continue
            if (child == toggle || child.visibility == GONE) continue
            layoutChildView(child)
        }
    }
    override fun onFinishInflate() {
        super.onFinishInflate()
        val toggle = if (::_toggle.isInitialized) { _toggle } else { getToggleView() }
        bringChildToFront(toggle)
        requestLayout()
        invalidate()
//        if (expanded) { autoCollapseTimer.begin() }
    }
    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
            CollapseLayoutParams(context, attrs)
    override fun generateLayoutParams(p: LayoutParams?): LayoutParams =
            CollapseLayoutParams(p)
    override fun checkLayoutParams(p: LayoutParams?): Boolean = p is CollapseLayoutParams

    private fun adjustForOvershoot(dimension: Int) = dimension * 12 / 10
    private fun layoutToggleView(view: View) {
        val lp = view.layoutParams as CollapseLayoutParams
        val w = view.measuredWidth
        val h = view.measuredHeight

        with (_containerRect) {
            left = _paddingRect.left + lp.leftMargin
            right = _paddingRect.right - lp.rightMargin
            top = _paddingRect.top + lp.topMargin
            bottom = _paddingRect.bottom - lp.bottomMargin
        }

        Gravity.apply(CollapseGravity.TOP_HAT, w, h, _containerRect, _toggleRect)
        with (_toggleRect) { view.layout(left, top, right, bottom) }

        // TODO: Might need to adjust these values
        val expandedTranslation = 0f // (_containerRect.top - _childRect.top).toFloat()
        val collapsedTranslation = (_containerRect.bottom - h).toFloat()

        with (view) {
            if (expanded) {
                translationY = expandedTranslation
            } else {
                translationX = collapsedTranslation
            }
        }
        lp.apply {
            setNonAlphaFloatValues(expandedTranslation, collapsedTranslation)
            setNonAlphaTarget(view)
        }
        _containerRect.top = 0
    }
    private fun layoutChildView(view: View) {
        val lp = view.layoutParams as CollapseLayoutParams
        val w = view.measuredWidth
        val h = view.measuredHeight
        val t = _containerRect.top

        with (_containerRect) {
            left = _paddingRect.left + lp.leftMargin
            right = _paddingRect.right - lp.rightMargin
            top = _paddingRect.top + lp.topMargin + t
            bottom = _paddingRect.bottom - lp.bottomMargin + t
        }

        Gravity.apply(Gravity.CENTER_VERTICAL, w, h, _containerRect, _childRect)
        with (_childRect) {
            view.layout(left, top, right, bottom)
        }

        // TODO: Might need to adjust these values
        val expandedTranslation =  0f //(_childRect.top - t).toFloat()
        val collapsedTranslation = (_containerRect.bottom - h).toFloat()

        with (view) {
            if (expanded) {
                translationY = expandedTranslation
                alpha = 1f
            } else {
                translationX = collapsedTranslation
                alpha = 0f
            }
        }
        lp.apply {
            setFloatValues(expandedTranslation, collapsedTranslation)
            setAnimatorTarget(view)
        }
        _containerRect.top = _containerRect.bottom  - (h * 1.1).roundToInt() //- _childRect.bottom
    }
    inner class CollapseLayoutParams : MarginLayoutParams {
        @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CollapseLayout_Layout)
            a.recycle()
        }
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: LayoutParams?) : super(source)

        private val toggleExpandAnimator: ObjectAnimator = ObjectAnimator()
        private val toggleCollapseAnimator: ObjectAnimator = ObjectAnimator()

        private val expandAnimator: ObjectAnimator = ObjectAnimator()
        private val expandAlphaAnimator: ObjectAnimator = ObjectAnimator()
        private val collapseAnimator: ObjectAnimator = ObjectAnimator()
        private val collapseAlphaAnimator: ObjectAnimator = ObjectAnimator()

        init {
            toggleExpandAnimator.interpolator = ExpandInterpolator
            toggleCollapseAnimator.interpolator = CollapseInterpolator

            expandAnimator.interpolator = ExpandInterpolator
            expandAlphaAnimator.interpolator = AlphaExpandInterpolator
            collapseAnimator.interpolator = CollapseInterpolator
            collapseAlphaAnimator.interpolator = CollapseInterpolator

            toggleExpandAnimator.setProperty(View.TRANSLATION_Y)
            toggleCollapseAnimator.setProperty(View.TRANSLATION_Y)

            expandAnimator.setProperty(View.TRANSLATION_Y)
            expandAlphaAnimator.apply {
                setProperty(View.ALPHA)
                setFloatValues(0f, 1f)
            }
            collapseAnimator.setProperty(View.TRANSLATION_Y)
            collapseAlphaAnimator.apply {
                setProperty(View.ALPHA)
                setFloatValues(1f, 0f)
            }
        }

        val gravity: Int
            get() = _gravity
        private var _gravity: Int = Gravity.CENTER_HORIZONTAL
        val position: Int
            get() = _position
        private var _position: Int = 0
        private var _ready: Boolean = false

        fun setNonAlphaFloatValues(expand: Float, collapsed: Float) {
            toggleExpandAnimator.setFloatValues(collapsed, expand)
            toggleCollapseAnimator.setFloatValues(expand, collapsed)
        }
        fun setFloatValues(expand: Float, collapsed: Float) {
            expandAnimator.setFloatValues(collapsed, expand)
            collapseAnimator.setFloatValues(expand, collapsed)
        }
        fun setNonAlphaTarget(view: View) {
            toggleExpandAnimator.target = view
            toggleCollapseAnimator.target = view
            prepareNonAlphaAnimations(view)
        }
        fun setAnimatorTarget(view: View) {
            expandAnimator.target = view
            expandAlphaAnimator.target = view
            collapseAnimator.target = view
            collapseAlphaAnimator.target = view
            prepareAnimations(view)
        }

        @Synchronized
        private fun prepareNonAlphaAnimations(view: View) {
            if (!_ready) {
                Log.i("LayoutParams", "Preparing non-alpha animations")
                addLayerTypeListener(toggleExpandAnimator, view)
                addLayerTypeListener(toggleCollapseAnimator, view)
                expandAnimation.playTogether(toggleExpandAnimator)
                collapseAnimation.playTogether(toggleCollapseAnimator)
                _ready = true
            }
        }
        @Synchronized
        private fun prepareAnimations(view: View) {
            if (!_ready) {
                addLayerTypeListener(expandAnimator, view)
                addLayerTypeListener(collapseAnimator, view)
                expandAnimation.playTogether(expandAnimator, expandAlphaAnimator)
                collapseAnimation.playTogether(collapseAnimator, collapseAlphaAnimator)
                _ready = true
            }
        }
        private fun addLayerTypeListener(animator: Animator, view: View) {
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }
                override fun onAnimationEnd(animation: Animator?) {
                    view.setLayerType(View.LAYER_TYPE_NONE, null)
                    invalidate()
                }
            })
        }
    }
    interface OnToggleStateChanged {
        fun onStateChanged()
    }
    companion object {
        object CollapseGravity {
            const val TOP_HAT: Int = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        private val ExpandInterpolator: Interpolator = OvershootInterpolator()
        private val CollapseInterpolator: Interpolator = DecelerateInterpolator(3f)
        private val AlphaExpandInterpolator: Interpolator = DecelerateInterpolator()
    }
}