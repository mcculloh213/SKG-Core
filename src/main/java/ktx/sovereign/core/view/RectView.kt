package ktx.sovereign.core.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import androidx.core.util.forEach
import androidx.core.util.set
import kotlin.random.Random

class RectView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint().also {
        it.style = Paint.Style.STROKE
        it.strokeWidth = 5f
    }

    private val rectMap: SparseArray<Rect> = SparseArray()
    private val colorMap: SparseIntArray = SparseIntArray()

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT)
        rectMap.forEach { i, rect ->
            paint.color = colorMap.get(i)
            canvas.drawRect(rect, paint)
        }
    }

    fun addRect(index: Int, rect: Rect) {
        rectMap[index] = rect
        colorMap[index] = nextColor()
        invalidate()
    }

    private fun nextColor(): Int =
        Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
}