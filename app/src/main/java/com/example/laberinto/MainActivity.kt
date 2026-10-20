package com.example.laberinto

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var mazeView: MazeView
    private lateinit var accelerometer: AccelerometerHandler
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Evita que la pantalla rote mientras se inclina el teléfono
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // ----- Interfaz creada por código (sin XML) -----
        mazeView = MazeView(this)

        val tvUuid = TextView(this).apply {
            text = "UUID: ${obtenerIdDispositivo()}"
            setTextColor(Color.parseColor("#CDD6F4"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E2E"))
            // El laberinto ocupa todo el espacio y el UUID queda abajo
            addView(mazeView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(tvUuid, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)

        // ----- Acelerómetro -----
        accelerometer = AccelerometerHandler(this) { ax, ay ->
            mazeView.accelX = ax
            mazeView.accelY = ay
        }

        // ----- Sonido al salir del laberinto -----
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        mazeView.onExit = {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
            Toast.makeText(this, "¡Saliste del laberinto!", Toast.LENGTH_SHORT).show()
            mazeView.postDelayed({ mazeView.reset() }, 2500)
        }
    }

    /** ID del dispositivo: secuencia hexadecimal de 16 caracteres. */
    /** ID del dispositivo: secuencia hexadecimal de 16 caracteres. */
    private fun obtenerIdDispositivo(): String =
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "No disponible"

    // La app está en uso -> se activa el sensor
    override fun onResume() {
        super.onResume()
        accelerometer.start()
    }

    // La app sale de primer plano -> se libera el sensor
    override fun onPause() {
        super.onPause()
        accelerometer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
    }
}