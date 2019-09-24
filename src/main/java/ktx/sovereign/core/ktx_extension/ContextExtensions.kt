@file:JvmName("ContextUtil")
package ktx.sovereign.core.ktx_extension

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService

@JvmOverloads
fun Context.closeSoftInputKeyboard(focus: View? = null): Boolean {
    val manager = getSystemService<InputMethodManager>() ?: return false
    val token = focus?.windowToken ?: View(this).windowToken
    return manager.hideSoftInputFromWindow(token, 0)
}

@RequiresApi(Build.VERSION_CODES.O)
fun Context.queryNetworkStatus(
        request: NetworkRequest,
        networkCallback: ConnectivityManager.NetworkCallback,
        handler: Handler
): Boolean {
    val manager = getSystemService<ConnectivityManager>() ?: return false
    manager.requestNetwork(request, networkCallback, handler)
    return true
}