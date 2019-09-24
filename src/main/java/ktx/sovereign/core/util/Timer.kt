package ktx.sovereign.core.util

import android.os.CountDownTimer


/**
 * Extendable [Timer] that implements the Android utility [CountDownTimer].
 *
 * Use the public function [begin] to start the [CountDownTimer], which will 'tick' every
 * 1,000 milliseconds (or every interval from the provided parameter), culminating once it has reached
 * the provided time limit.
 *
 * Once the limit has been hit, it will invoke the method [onFinish], which will then run the
 * supplied callback, if it exists.
 *
 * Using the public function [end] will cancel the running timer, and it will not invoke the
 * method [onFinish].
 *
 * @param millis    The number of milliseconds to run the timer for.
 * @param tick      The millisecond interval at which the [onTick] callback will be invoked. It is
 *                  set to a default of 1,000 milliseconds, or 1 second. For long running
 *                  operations, this should be sufficient enough, although it is important to note
 *                  that this is a configurable field.
 * @param onFinish  The callback function to invoke when the [CountDownTimer] has successfully
 *                  finished. It is important to note that there is no default function, and
 *                  the timer will not do anything worthwhile.
 */
open class Timer(
        millis: Long,
        tick: Long = 1_000,
        private val onFinish: (() -> Unit)? = null
) : CountDownTimer(millis, tick) {
    var running: Boolean = false
        private set(value) {
            field = value
        }

    /**
     * Starts the [CountDownTimer], additionally setting the public read-only field, [running]
     * to `true`.
     */
    fun begin() {
        running = true
        start()
    }

    fun reset() {
        cancel()
        start()
    }

    /**
     * Cancels the [CountDownTimer], additionally setting the public read-only field, [running]
     * to `false`.
     *
     * The method [onFinish] will not be invoked when the timer is cancelled.
     */
    fun end() {
        cancel()
        running = false
    }

    override fun onTick(millisUntilFinished: Long) {
        if (!running) {
            running = true
        }
    }

    final override fun onFinish() {
        running = false
        onFinish?.invoke()
    }
}