package com.example.laberinto

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Encapsula el acelerómetro.
 * start() -> empieza a escuchar (cuando la app está en uso).
 * stop()  -> libera el sensor (cuando la app sale de primer plano).
 */
class AccelerometerHandler(
    context: Context,
    private val onChange: (ax: Float, ay: Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var running = false

    fun start() {
        if (running) return
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            running = true
        }
    }

    fun stop() {
        if (!running) return
        sensorManager.unregisterListener(this)
        running = false
        onChange(0f, 0f)   // la pelota deja de recibir fuerza
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // En vertical: inclinar a la derecha => values[0] negativo
            onChange(-event.values[0], event.values[1])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}