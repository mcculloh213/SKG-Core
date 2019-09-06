package ktx.sovereign.core.util

import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * LogMAR or Log Minimum Angle of Resolution
 *
 * For any Logarithm with base `b`,
 *      log_b (x) = y iff b^y = x
 * @param defaultSize   The baseline size for LogMAR(0)
 * @param infimum       The lower bound
 * @param supremum      The upper bound
 */
class LogMAR @JvmOverloads constructor(
    private val defaultSize: Float,
    private val infimum: Float = ZERO,
    private val supremum: Float = 1.5f
) {
    companion object {
        private const val ZERO: Float = 0.0f
        private const val TEN: Float = 10.00f
        private const val STEP: Float = 0.1f
    }
    private var _exp: Float = 0.0f
    val value: Float
        get() = _exp
    val scale: Float
        get() = defaultSize * TEN.pow(_exp)
    val minScale: Float
        get() = defaultSize * TEN.pow(infimum)
    val maxScale: Float
        get() = defaultSize * TEN.pow(supremum)

    @JvmOverloads
    fun stepUp(multiplier: Int = 1): Float {
        _exp = min(supremum, _exp + (STEP * multiplier))
        return scale
    }
    @JvmOverloads
    fun stepDown(multiplier: Int = 1): Float {
        _exp = max(infimum, _exp - (STEP * multiplier))
        return scale
    }
    fun stepTo(level: Int): Float {
        Log.d("LogMAR", "Step to: $level")
        _exp = min(supremum, ZERO + (STEP * level))
        return scale
    }
    fun reset() {
        _exp = ZERO
    }
}