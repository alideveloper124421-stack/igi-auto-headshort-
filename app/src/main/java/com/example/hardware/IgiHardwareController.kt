package com.example.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.input.InputManager
import android.media.MediaPlayer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.model.GamepadSnapshot
import com.example.model.GyroSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.sign

class IgiHardwareController(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager

    private val _gyroSnapshot = MutableStateFlow(GyroSnapshot(available = gyroSensor != null))
    val gyroSnapshot: StateFlow<GyroSnapshot> = _gyroSnapshot.asStateFlow()

    private val _gamepadSnapshot = MutableStateFlow(GamepadSnapshot())
    val gamepadSnapshot: StateFlow<GamepadSnapshot> = _gamepadSnapshot.asStateFlow()

    private var lastGyroTimestampNs: Long = 0L
    private var activeGamepadId: Int = -1
    private var prevHatX: Int = 0
    private var prevHatY: Int = 0
    private var rainPlayer: MediaPlayer? = null
    private var isRegistered = false

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!_gyroSnapshot.value.enabled) return
            if (lastGyroTimestampNs == 0L) {
                lastGyroTimestampNs = event.timestamp
                return
            }
            val dt = (event.timestamp - lastGyroTimestampNs) / 1_000_000_000f
            lastGyroTimestampNs = event.timestamp
            if (dt <= 0f || dt > 0.1f) return
            val yawDelta = -event.values[0] * dt
            val pitchDelta = -event.values[1] * dt
            _gyroSnapshot.update {
                it.copy(
                    yawRadians = (it.yawRadians + yawDelta).coerceIn(-1.5f, 1.5f),
                    pitchRadians = (it.pitchRadians + pitchDelta).coerceIn(-1.2f, 1.2f)
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val gamepadListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refreshGamepadPresence()
        override fun onInputDeviceRemoved(deviceId: Int) {
            if (deviceId == activeGamepadId) {
                activeGamepadId = -1
                prevHatX = 0
                prevHatY = 0
            }
            refreshGamepadPresence()
        }

        override fun onInputDeviceChanged(deviceId: Int) = refreshGamepadPresence()
    }

    fun start() {
        if (isRegistered) return
        isRegistered = true
        if (gyroSensor != null) {
            sensorManager?.registerListener(gyroListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        inputManager?.registerInputDeviceListener(gamepadListener, null)
        refreshGamepadPresence()
    }

    fun stop() {
        if (!isRegistered) return
        isRegistered = false
        sensorManager?.unregisterListener(gyroListener)
        inputManager?.unregisterInputDeviceListener(gamepadListener)
        lastGyroTimestampNs = 0L
        pauseRainAudio()
    }

    fun setGyroEnabled(enabled: Boolean) {
        _gyroSnapshot.update {
            it.copy(enabled = enabled, yawRadians = 0f, pitchRadians = 0f)
        }
    }

    fun resetGyroOrientation() {
        _gyroSnapshot.update { it.copy(yawRadians = 0f, pitchRadians = 0f) }
    }

    fun updateRainAudio(shouldPlay: Boolean) {
        if (shouldPlay) {
            startRainAudio()
        } else {
            pauseRainAudio()
        }
    }

    private fun startRainAudio() {
        try {
            if (rainPlayer == null) {
                val afd = context.assets.openFd("sounds/rain-loop.wav")
                rainPlayer = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    isLooping = true
                    setVolume(0.32f, 0.32f)
                    prepare()
                }
            }
            if (rainPlayer?.isPlaying == false) {
                rainPlayer?.start()
            }
        } catch (_: Exception) {
        }
    }

    private fun pauseRainAudio() {
        try {
            if (rainPlayer?.isPlaying == true) {
                rainPlayer?.pause()
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        try {
            rainPlayer?.release()
        } catch (_: Exception) {
        }
        rainPlayer = null
    }

    fun refreshGamepadPresence() {
        var foundId = -1
        var foundName = "No controller connected"
        if (activeGamepadId != -1 && InputDevice.getDevice(activeGamepadId) != null) {
            foundId = activeGamepadId
            foundName = InputDevice.getDevice(foundId)?.name ?: "Gamepad #$foundId"
        } else {
            for (id in InputDevice.getDeviceIds()) {
                val dev = InputDevice.getDevice(id) ?: continue
                if (isGamepadDevice(dev)) {
                    foundId = id
                    foundName = dev.name ?: "Gamepad #$id"
                    break
                }
            }
        }
        activeGamepadId = foundId
        _gamepadSnapshot.update {
            it.copy(
                connected = foundId != -1,
                deviceId = foundId,
                deviceName = foundName
            )
        }
    }

    fun onKeyEvent(keyCode: Int, event: KeyEvent, isDown: Boolean): Boolean {
        if (isGamepadButtonKeyCode(keyCode)) {
            if (activeGamepadId == -1 && event.deviceId != -1) {
                activeGamepadId = event.deviceId
                refreshGamepadPresence()
            }
        }
        if (isGamepadKeyCode(keyCode)) {
            if (activeGamepadId != -1 && event.deviceId != activeGamepadId) return false
            val actionLabel = "${KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")} ${if (isDown) "DOWN" else "UP"}"
            _gamepadSnapshot.update { it.copy(lastInputLabel = actionLabel) }
            return true
        }
        return false
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        val fromAdoptedPad = activeGamepadId != -1 && event.deviceId == activeGamepadId
        val isGamepadSource = (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (!fromAdoptedPad && !isGamepadSource) return false
        if (activeGamepadId != -1 && event.deviceId != activeGamepadId) return false
        if (activeGamepadId == -1) refreshGamepadPresence()

        val lx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X))
        val ly = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y))
        var rx = event.getAxisValue(MotionEvent.AXIS_Z)
        var ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        if (rx == 0f) rx = event.getAxisValue(MotionEvent.AXIS_RX)
        if (ry == 0f) ry = event.getAxisValue(MotionEvent.AXIS_RY)
        rx = applyDeadzone(rx)
        ry = applyDeadzone(ry)

        var lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        var rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        if (lt == 0f) lt = event.getAxisValue(MotionEvent.AXIS_BRAKE)
        if (rt == 0f) rt = event.getAxisValue(MotionEvent.AXIS_GAS)

        val hatX = sign(event.getAxisValue(MotionEvent.AXIS_HAT_X)).toInt()
        val hatY = sign(event.getAxisValue(MotionEvent.AXIS_HAT_Y)).toInt()
        var hatLabel = _gamepadSnapshot.value.lastInputLabel
        if (hatX != prevHatX || hatY != prevHatY) {
            hatLabel = "D-PAD ($hatX, $hatY)"
            prevHatX = hatX
            prevHatY = hatY
        }

        _gamepadSnapshot.update {
            it.copy(
                leftX = lx,
                leftY = ly,
                rightX = rx,
                rightY = ry,
                leftTrigger = lt,
                rightTrigger = rt,
                lastInputLabel = hatLabel
            )
        }
        return true
    }

    private fun applyDeadzone(value: Float): Float = if (abs(value) < 0.18f) 0f else value

    private fun isGamepadDevice(device: InputDevice): Boolean {
        val sources = device.sources
        return (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    private fun isGamepadButtonKeyCode(keyCode: Int): Boolean = keyCode in setOf(
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_SELECT
    )

    private fun isGamepadKeyCode(keyCode: Int): Boolean =
        isGamepadButtonKeyCode(keyCode) || keyCode in setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT
        )
}
