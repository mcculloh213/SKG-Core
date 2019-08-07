package ktx.sovereign.core.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class TiltScrollController(
    context: Context,
    private val listener: TiltScrollListener
) : SensorEventListener {
    companion object {
        @JvmStatic val MOTION_THRESHOLD: Float = 0.001f
        @JvmStatic val SENSOR_DELAY_MILLISECONDS: Int = 50 * 1000 // := 32 ms
    }

    private val mSensorManager: SensorManager = context.getSystemService(SensorManager::class.java)
    private val mWindowManager: WindowManager = context.getSystemService(WindowManager::class.java)
    private val mRotationSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var initialized: Boolean = false
    private var mCurrentAccuracy: Int = -1
    private var x: Float = -1.0f
    private var z: Float = -1.0f

    private val mRotationMatrix: FloatArray = FloatArray(9)
    private val mAdjustedRotationMatrix: FloatArray = FloatArray(9)
    private val mOrientationVector: FloatArray = FloatArray(3)

    private var _freeze: Boolean = false
    val frozen: Boolean
        get() = _freeze

    init {
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MILLISECONDS)
    }

    fun requestRotationSensor() {
        if (_freeze) {
            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MILLISECONDS)
            _freeze = false
        }
    }
    fun releaseRotationSensor() {
        if (!_freeze) {
            mSensorManager.unregisterListener(this, mRotationSensor)
            _freeze = true
        }
    }
    fun toggleFreeze() {
        if (_freeze) {
            requestRotationSensor()
        } else {
            releaseRotationSensor()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (mCurrentAccuracy != accuracy) {
            mCurrentAccuracy = accuracy
        }
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (mCurrentAccuracy != SensorManager.SENSOR_STATUS_UNRELIABLE) {
            event?.apply {
                if (sensor == mRotationSensor) {
                    updateOrientation(values.clone())
                }
            }
        }
    }
    private fun updateOrientation(rotationVector: FloatArray) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, rotationVector)

        val xAxis: Int
        val yAxis: Int

        when (mWindowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> {
                xAxis = SensorManager.AXIS_X
                yAxis = SensorManager.AXIS_Z
            }
            Surface.ROTATION_90 -> {
                xAxis = SensorManager.AXIS_Z
                yAxis = SensorManager.AXIS_MINUS_X
            }
            Surface.ROTATION_180 -> {
                xAxis = SensorManager.AXIS_MINUS_X
                yAxis = SensorManager.AXIS_MINUS_Z
            }
            Surface.ROTATION_270 -> {
                xAxis = SensorManager.AXIS_MINUS_Z
                yAxis = SensorManager.AXIS_X
            }
            else -> {
                xAxis = SensorManager.AXIS_X
                yAxis = SensorManager.AXIS_Z
            }
        }

        SensorManager.remapCoordinateSystem(mRotationMatrix, xAxis, yAxis, mAdjustedRotationMatrix)
        SensorManager.getOrientation(mAdjustedRotationMatrix, mOrientationVector)

        val degX = Math.toDegrees(mOrientationVector[1].toDouble()).toFloat()
        val degZ = Math.toDegrees(mOrientationVector[0].toDouble()).toFloat()

        var dX = applyThreshold(angularRounding(degX - x))
        var dZ = applyThreshold(angularRounding(degZ - z))

        if (!initialized) {
            dX = 0f
            dZ = 0f
            initialized = true
        }

        x = degX
        z = degZ

        listener.onTilt(dZ.toInt() * 60, dX.toInt() * 60)
    }
    private fun applyThreshold(value: Float): Float {
        return if (abs(value) > MOTION_THRESHOLD) {
            value
        } else {
            0f
        }
    }
    private fun angularRounding(value: Float): Float {
        return when {
            value >= 180.0f -> value - 360.0f
            value <= -180.0f -> 360.0f + value
            else -> value
        }
    }

    interface TiltScrollListener {
        /**
         * Called when the element should scroll
         *
         * @param x The distance to scroll on the X axis
         * @param y The distance to scroll on the Y axis
         */
        fun onTilt(x: Int, y: Int)
    }
}