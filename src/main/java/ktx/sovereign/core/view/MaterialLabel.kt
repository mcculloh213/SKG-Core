package ktx.sovereign.core.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.material_label.view.*
import ktx.sovereign.core.R

class MaterialLabel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    init {
        inflate(context, R.layout.material_label, this)
        clipChildren = false
        clipToPadding = false

        val a = context.obtainStyledAttributes(attrs, R.styleable.MaterialLabel, defStyleAttr, defStyleRes)
        a.style()
        a.recycle()
    }

    fun setText(sequence: CharSequence?) {
        text.text = sequence
    }

    private fun TypedArray.style() {
        text.text = getString(R.styleable.MaterialLabel_text)
    }
}
