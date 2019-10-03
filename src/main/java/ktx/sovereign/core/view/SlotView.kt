package ktx.sovereign.core.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.res.ResourcesCompat
import ktx.sovereign.core.R
import kotlin.math.max
import kotlin.math.min

class SlotView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : LinearLayoutCompat(context, attrs, defStyle) {
    companion object {
        @JvmStatic fun zoomFromInt(value: Int): ZoomLevel {
            return ZoomLevel.values().firstOrNull { it.value == value }
                ?: ZoomLevel.ZERO
        }
        @JvmStatic fun zoomNextLevel(current: ZoomLevel): ZoomLevel {
            return ZoomLevel.values().firstOrNull { it.value == (current.value+1) }
                ?: current
        }
        @JvmStatic fun zoomNextLevel(current: Int): ZoomLevel {
            return ZoomLevel.values().firstOrNull { it.value == (current+1) }
                ?: zoomFromInt(current)
        }
        @JvmStatic fun zoomPreviousLevel(current: ZoomLevel): ZoomLevel {
            return ZoomLevel.values().firstOrNull { it.value == (current.value-1) }
                ?: current
        }
        @JvmStatic fun zoomPreviousLevel(current: Int): ZoomLevel {
            return ZoomLevel.values().firstOrNull { it.value == (current-1) }
                ?: zoomFromInt(current)
        }
        const val DEFAULT_SLOTS: Int = 10
        @JvmField val DEFAULT_ZOOM_LEVEL: ZoomLevel = ZoomLevel.ZERO
        @JvmField val DEFAULT_MIN_ZOOM: ZoomLevel = ZoomLevel.ZERO
        @JvmField val DEFAULT_MAX_ZOOM: ZoomLevel = ZoomLevel.TEN
    }

    val currentLevel: Int
        get() = currentZoom.value

    private val slots: Int
    private val filledSlot: Drawable
    private val emptySlot: Drawable
    private var currentZoom: ZoomLevel
    private val minZoomLevel: ZoomLevel
    private val maxZoomLevel: ZoomLevel
    private var listener: OnClickSlotListener? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SlotView, defStyle, 0)
        slots = a.getInt(R.styleable.SlotView_slots, DEFAULT_SLOTS)
        filledSlot = ResourcesCompat.getDrawable(resources, R.drawable.sh_circle_filled, context.theme)!!
        emptySlot = ResourcesCompat.getDrawable(resources, R.drawable.sh_circle_empty, context.theme)!!
        currentZoom = zoomFromInt(a.getInt(R.styleable.SlotView_default_slot_level, DEFAULT_ZOOM_LEVEL.value))
        minZoomLevel = zoomFromInt(a.getInt(R.styleable.SlotView_min_slot_level, DEFAULT_MIN_ZOOM.value))
        maxZoomLevel = zoomFromInt(a.getInt(R.styleable.SlotView_max_slot_level, DEFAULT_MAX_ZOOM.value))
        a.recycle()
        weightSum = slots.toFloat()
        isSaveEnabled = true
    }

    fun setCurrentZoomLevel(level: ZoomLevel) {
        currentZoom = level
        drawSlots()
    }
    fun increaseZoomLevel() {
        val proposed = zoomNextLevel(currentZoom)
        currentZoom = zoomFromInt(min(maxZoomLevel.value, proposed.value))
        drawSlots()
    }
    fun decreaseZoomLevel() {
        val proposed = zoomPreviousLevel(currentZoom)
        currentZoom = zoomFromInt(max(minZoomLevel.value, proposed.value))
        drawSlots()
    }
    fun setOnClickSlotListener(listener: OnClickSlotListener) {
        this@SlotView.listener = listener
    }
    fun setOnClickSlotListener(listener: OnClickSlotListener_Impl.() -> Unit) {
        val l = OnClickSlotListener_Impl()
        listener.invoke(l)
        this@SlotView.listener = l
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        drawSlots()
    }
    override fun onSaveInstanceState(): Parcelable? {
        return SaveState(super.onSaveInstanceState(), currentZoom)
    }
    override fun onRestoreInstanceState(state: Parcelable?) {
        val saved = state as SaveState
        super.onRestoreInstanceState(state)
        currentZoom = saved.zoomLevel
        drawSlots()
        invalidate()
    }

    private fun drawSlots() {
        if (childCount == 0) {
            createSlots()
        }
        updateSlots()
    }
    private fun createSlots() {
        for (i in 0 until slots) {
            insertSlot(context, i)
        }
    }
    private fun insertSlot(context: Context, index: Int) {
        with (ImageView(context)) {
            setImageDrawable(emptySlot)
            contentDescription = "hf_no_number|Zoom Level ${index+1}"
            setOnClickListener(OnClickListener(index))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f)
            this@SlotView.addView(this@with)
        }
    }
    private fun updateSlots() {
        val level = getFilledCount()
        for (i in 0 until slots) {
            if (i < level) {
                fillSlot(i)
            } else {
                clearSlot(i)
            }
        }
    }
    private fun fillSlot(index: Int) {
        (getChildAt(index) as ImageView).setImageDrawable(filledSlot)
    }
    private fun clearSlot(index: Int) {
        (getChildAt(index) as ImageView).setImageDrawable(emptySlot)
    }
    private fun getFilledCount(): Int = currentZoom.value

    enum class ZoomLevel(val value: Int) {
        ZERO(0), ONE(1), TWO(2),
        THREE(3), FOUR(4), FIVE(5),
        SIX(6), SEVEN(7), EIGHT(8),
        NINE(9), TEN(10)
    }
    interface OnClickSlotListener {
        fun onClickSlot(value: Int)
    }
    class OnClickSlotListener_Impl : OnClickSlotListener {
        var onClick: ((value: Int) -> Unit)? = null
        override fun onClickSlot(value: Int) {
            onClick?.invoke(value)
        }
    }
    internal class SaveState : BaseSavedState {
        companion object {
            @JvmField val CREATOR = object : Parcelable.Creator<SaveState> {
                override fun createFromParcel(source: Parcel?): SaveState {
                    return SaveState(source)
                }
                override fun newArray(size: Int): Array<SaveState?> {
                    return arrayOfNulls(size)
                }
            }
        }
        val zoomLevel: ZoomLevel
            get() = zoomFromInt(current)
        private val current: Int

        constructor(superState: Parcelable?, currentZoom: ZoomLevel) : super(superState) {
            current = currentZoom.value
        }
        constructor(`in`: Parcel?) : super(`in`) {
            current = `in`?.readInt() ?: DEFAULT_MIN_ZOOM.value
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeInt(current)
        }
    }
    inner class OnClickListener(private val idx: Int) : View.OnClickListener {
        override fun onClick(view: View?) {
            Log.d("SlotView", "Index: $idx")
            setCurrentZoomLevel(zoomFromInt(idx+1))
            listener?.onClickSlot(idx+1)
        }
    }
}