package ktx.sovereign.core.groupie

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_material_card.view.*
import ktx.sovereign.core.R
import ktx.sovereign.database.entity.ScrollingMenuItem
import java.util.*

open class MaterialCardItem : Item {
    @DrawableRes
    private val iconResId: Int
    @StringRes
    private var labelResId: Int? = null
    private lateinit var _label: CharSequence
    @StringRes
    private var directiveResId: Int? = null
    private lateinit var _directive: CharSequence
    private var _tint: Int = Color.TRANSPARENT

    val launch: String
        get() = if (::_launch.isInitialized) {
            _launch
        } else {
            "noop"
        }
    private lateinit var _launch: String

    constructor(@DrawableRes icon: Int, @StringRes label: Int) : super() {
        iconResId = icon
        labelResId = label
    }
    constructor(@DrawableRes icon: Int, label: CharSequence) : super() {
        iconResId = icon
        _label = label
    }
    constructor(item: ScrollingMenuItem) : super() {
        iconResId = getDrawableResource(item.icon)
        _label = item.label
        _directive = String.format(Locale.US, item.directive, item.label)
        _tint = item.tint
        _launch = item.item
    }

    override fun getLayout(): Int = R.layout.item_material_card
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.apply {
            with(icon) {
                setImageResource(iconResId)
                setColorFilter(_tint)
            }
            label.text = getCardLabel(context)
            contentDescription = getDirective(position)
        }
    }

    private fun getCardLabel(context: Context): CharSequence = if (::_label.isInitialized) {
        _label
    } else {
        labelResId?.let { res ->
            context.getString(res).also {
                _label = it
            }
        } ?: "NO LABEL"
    }
    private fun getDirective(position: Int): CharSequence = if (::_directive.isInitialized) {
        _directive
    } else {
        "hf_no_number|hf_show_text|Select Menu $position"
    }
    private fun getDrawableResource(name: String): Int {
        var res = -1
        try {
            val f = R.drawable::class.java.getDeclaredField(name)
            res = f.getInt(name)
        } catch (ex: Exception) {
            Log.e("Reflection", "$ex")
            ex.printStackTrace()
        } finally {
            return res
        }
    }
}