package com.example.laberinto

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.util.ArrayDeque
import kotlin.math.min
import kotlin.random.Random

/**
 * Genera el laberinto y se encarga de su DIBUJO por capas con formas básicas.
 * El laberinto se genera con un algoritmo (backtracking), por lo que siempre
 * tiene solución. La pelota empieza en el centro y la salida es un hueco en
 * el borde, en el punto más lejano del centro.
 */
class MazeDrawer {

    // Dimensiones IMPARES: las celdas de pasillo caen en índices impares.
    val cols = 15
    val rows = 23

    // true = pared, false = pasillo
    private val grid = Array(rows) { BooleanArray(cols) { true } }

    // Celda de inicio: el centro del laberinto
    val startRow = rows / 2   // 11
    val startCol = cols / 2   // 7

    // Celda de salida (hueco en el borde)
    var exitRow = 0; private set
    var exitCol = 0; private set

    // Geometría calculada según el tamaño de la pantalla
    var cell = 0f; private set
    var ox = 0f; private set
    var oy = 0f; private set
    val radius: Float get() = cell * 0.3f

    // Una pintura por capa
    private val backgroundPaint = Paint().apply { color = Color.parseColor("#1E1E2E") }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#89B4FA") }
    private val exitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A6E3A1") }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F38BA8") }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 0, 0, 0) }

    init {
        generateMaze(Random(7))   // cambia el número para obtener otro laberinto
        openExit()
    }

    // ---------------------------------------------------------------
    //  Generación del laberinto
    // ---------------------------------------------------------------
    private fun generateMaze(random: Random) {
        val stack = ArrayDeque<IntArray>()
        grid[startRow][startCol] = false
        stack.push(intArrayOf(startRow, startCol))
        val dirs = arrayOf(intArrayOf(0, 2), intArrayOf(0, -2), intArrayOf(2, 0), intArrayOf(-2, 0))

        while (stack.isNotEmpty()) {
            val (r, c) = stack.peek()
            val options = dirs.map { intArrayOf(r + it[0], c + it[1]) }.filter {
                it[0] in 1 until rows - 1 && it[1] in 1 until cols - 1 && grid[it[0]][it[1]]
            }
            if (options.isEmpty()) {
                stack.pop()
            } else {
                val next = options[random.nextInt(options.size)]
                grid[(r + next[0]) / 2][(c + next[1]) / 2] = false  // derriba la pared intermedia
                grid[next[0]][next[1]] = false
                stack.push(next)
            }
        }
    }

    /** Abre la salida en el borde, junto a la celda más lejana del centro. */
    private fun openExit() {
        // Distancias desde el centro (BFS)
        val dist = Array(rows) { IntArray(cols) { -1 } }
        val queue = ArrayDeque<IntArray>()
        dist[startRow][startCol] = 0
        queue.add(intArrayOf(startRow, startCol))
        while (queue.isNotEmpty()) {
            val (r, c) = queue.poll()
            for (d in arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))) {
                val nr = r + d[0]
                val nc = c + d[1]
                if (nr in 0 until rows && nc in 0 until cols && !grid[nr][nc] && dist[nr][nc] == -1) {
                    dist[nr][nc] = dist[r][c] + 1
                    queue.add(intArrayOf(nr, nc))
                }
            }
        }

        // Candidatos: huecos posibles en los cuatro bordes
        var best = -1
        var bestRow = 0
        var bestCol = 0
        fun check(borderR: Int, borderC: Int, innerR: Int, innerC: Int) {
            if (dist[innerR][innerC] > best) {
                best = dist[innerR][innerC]
                bestRow = borderR
                bestCol = borderC
            }
        }
        for (r in 1 until rows - 1 step 2) {
            check(r, 0, r, 1)                    // borde izquierdo
            check(r, cols - 1, r, cols - 2)      // borde derecho
        }
        for (c in 1 until cols - 1 step 2) {
            check(0, c, 1, c)                    // borde superior
            check(rows - 1, c, rows - 2, c)      // borde inferior
        }

        exitRow = bestRow
        exitCol = bestCol
        grid[exitRow][exitCol] = false
    }

    // ---------------------------------------------------------------
    //  Consultas y geometría
    // ---------------------------------------------------------------
    fun updateSize(w: Int, h: Int) {
        cell = min(w.toFloat() / cols, h.toFloat() / rows)
        ox = (w - cell * cols) / 2f
        oy = (h - cell * rows) / 2f
    }

    fun isWall(r: Int, c: Int) = grid[r][c]

    /** ¿La pelota ya salió del área del laberinto? */
    fun isOutside(x: Float, y: Float) =
        x < ox || x > ox + cols * cell || y < oy || y > oy + rows * cell

    // ---------------------------------------------------------------
    //  Dibujo por capas
    // ---------------------------------------------------------------
    fun draw(canvas: Canvas, w: Int, h: Int, ballX: Float, ballY: Float) {
        drawBackgroundLayer(canvas, w, h)    // Capa 1: fondo
        drawWallsLayer(canvas)               // Capa 2: paredes
        drawExitLayer(canvas)                // Capa 3: salida
        drawBallLayer(canvas, ballX, ballY)  // Capa 4: pelota
    }

    private fun drawBackgroundLayer(canvas: Canvas, w: Int, h: Int) {
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), backgroundPaint)
    }

    private fun drawWallsLayer(canvas: Canvas) {
        for (r in 0 until rows) for (c in 0 until cols) {
            if (grid[r][c]) {
                val l = ox + c * cell
                val t = oy + r * cell
                canvas.drawRect(l, t, l + cell, t + cell, wallPaint)
            }
        }
    }

    private fun drawExitLayer(canvas: Canvas) {
        val l = ox + exitCol * cell
        val t = oy + exitRow * cell
        canvas.drawRect(l, t, l + cell, t + cell, exitPaint)
    }

    private fun drawBallLayer(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x + radius * 0.15f, y + radius * 0.15f, radius, shadowPaint)
        canvas.drawCircle(x, y, radius, ballPaint)
    }
}