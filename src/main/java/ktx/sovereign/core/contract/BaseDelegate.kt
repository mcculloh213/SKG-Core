package ktx.sovereign.core.contract

import android.view.View
import androidx.annotation.IdRes

interface BaseDelegate {
    fun createExtraMenu(menu: View)
    fun removeExtraMenu()
    fun requestNavigationEvent(@IdRes to: Int)
    fun requestClearNavigationStack()
}