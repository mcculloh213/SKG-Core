package ktx.sovereign.core.contract

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import kotlinx.android.synthetic.main.layout_mobile_root.*
import kotlinx.android.synthetic.main.layout_mobile_root.view.*
import ktx.sovereign.core.R

abstract class BaseModule : AppCompatActivity(), BaseDelegate {
    companion object {
        val EXTRA_MODE: String = "com.industrialbadger.app.extra.MODE"
        const val MODE_MOBILE: Int = 0b01
        const val MODE_AR: Int = 0b10
    }

    fun configureActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun setContentView(@LayoutRes contentResId: Int) {
        with(layoutInflater) {
            val view = when (intent.getIntExtra(EXTRA_MODE, MODE_MOBILE)) {
                MODE_MOBILE -> inflate(R.layout.layout_mobile_root, null)
                else -> inflate(R.layout.layout_mobile_root, null)
            }
            inflate(contentResId, view.content_container, true)
            super.setContentView(view)
        }
    }

    protected fun setContentView(@LayoutRes contentResId: Int, @LayoutRes drawerResId: Int) {
        with(layoutInflater) {
            val view = when (intent.getIntExtra(EXTRA_MODE, MODE_MOBILE)) {
                MODE_MOBILE -> inflate(R.layout.layout_mobile_root, null)
                else -> inflate(R.layout.layout_mobile_root, null)
            }
            inflate(contentResId, content_container, true)
            inflate(drawerResId, drawer_container, true)
            super.setContentView(view)
        }
    }

    fun closeSoftInputKeyboard(): Boolean {
        val manager: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            ?: return false
        val view = currentFocus ?: View(this@BaseModule)
        return manager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun createExtraMenu(menu: View) {
        fab_menu_container.addView(menu)
    }

    override fun removeExtraMenu() {
        fab_menu_container.apply {
            children.forEach {
                removeView(it)
            }
        }
    }

    override fun requestNavigationEvent(to: Int) {}
    override fun requestClearNavigationStack() {}
}