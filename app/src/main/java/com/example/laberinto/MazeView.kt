package com.example.laberinto

import android.content.Context
import android.graphics.Canvas
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Vista del juego: mueve la pelota, detecta colisiones con las paredes
 * y avisa cuando la pelota sale del laberinto. El dibujo lo hace MazeDrawer.
 */
class MazeView(context: Context) : View(context) {

    /** Se llama cuando la pelota sale del laberinto. */
    var onExit: (() -> Unit)? = null

    /** Aceleración en pantalla (la asigna el acelerómetro). */
    var accelX = 0f
    var accelY = 0f

    private val drawer = MazeDrawer()

    private var x = 0f
    private var y = 0f
    private var vx = 0f
    private var vy = 0f
    private var finished = false
    private var lastTime = 0L

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        drawer.updateSize(w, h)
        reset()
    }

    /** Coloca la pelota en el CENTRO del laberinto. */
    fun reset() {
        x = drawer.ox + (drawer.startCol + 0.5f) * drawer.cell
        y = drawer.oy + (drawer.startRow + 0.5f) * drawer.cell
        vx = 0f
        vy = 0f
        finished = false
        lastTime = System.nanoTime()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!finished) updatePhysics()
        drawer.draw(canvas, width, height, x, y)
        if (!finished) postInvalidateOnAnimation()
    }

    private fun updatePhysics() {
        val now = System.nanoTime()
        val dt = min((now - lastTime) / 1_000_000_000f, 0.03f)
        lastTime = now

        val cell = drawer.cell
        val steps = 3                       // sub-pasos para no atravesar paredes
        val h = dt / steps
        val maxSpeed = cell * 7f

        for (i in 0 until steps) {
            vx += accelX * cell * 2.2f * h
            vy += accelY * cell * 2.2f * h
            vx = (vx * 0.995f).coerceIn(-maxSpeed, maxSpeed)
            vy = (vy * 0.995f).coerceIn(-maxSpeed, maxSpeed)

            x += vx * h
            y += vy * h

            resolveWallCollisions()

            // ¿Salió por el hueco del borde?
            if (drawer.isOutside(x, y)) {
                finished = true
                onExit?.invoke()
                return
            }
        }
    }

    /** Colisión círculo vs. rectángulo, solo con las paredes cercanas a la pelota. */
    private fun resolveWallCollisions() {
        val cell = drawer.cell
        val radius = drawer.radius

        val c0 = ((x - radius - drawer.ox) / cell).toInt().coerceIn(0, drawer.cols - 1)
        val c1 = ((x + radius - drawer.ox) / cell).toInt().coerceIn(0, drawer.cols - 1)
        val r0 = ((y - radius - drawer.oy) / cell).toInt().coerceIn(0, drawer.rows - 1)
        val r1 = ((y + radius - drawer.oy) / cell).toInt().coerceIn(0, drawer.rows - 1)

        for (r in r0..r1) for (c in c0..c1) {
            if (!drawer.isWall(r, c)) continue

            val left = drawer.ox + c * cell
            val top = drawer.oy + r * cell
            val closestX = x.coerceIn(left, left + cell)
            val closestY = y.coerceIn(top, top + cell)
            val dx = x - closestX
            val dy = y - closestY
            val dist2 = dx * dx + dy * dy

            if (dist2 < radius * radius && dist2 > 1e-6f) {
                val dist = sqrt(dist2)
                val nx = dx / dist
                val ny = dy / dist
                val overlap = radius - dist

                // Sacar la pelota de la pared
                x += nx * overlap
                y += ny * overlap

                // Rebote con pérdida de energía
                val vn = vx * nx + vy * ny
                if (vn < 0) {
                    val bounce = 0.3f
                    vx -= (1 + bounce) * vn * nx
                    vy -= (1 + bounce) * vn * ny
                }
            }
        }
    }
}