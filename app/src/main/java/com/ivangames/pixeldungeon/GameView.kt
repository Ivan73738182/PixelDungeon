package com.ivangames.pixeldungeon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    private val paint = Paint().apply { isAntiAlias = true }

    // Игрок
    private var playerX = 0f
    private var playerY = 0f
    private val playerSize = 80f

    // Джойстики
    private lateinit var moveJoystick: Joystick
    private lateinit var shootJoystick: Joystick

    // Скорость игрока
    private val playerSpeed = 8f

    // Пули (пока пусто)
    private val bullets = mutableListOf<Bullet>()

    // Время
    private var lastTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Центр игрока
        playerX = width / 2f
        playerY = height / 2f

        // Джойстики
        moveJoystick = Joystick(
            width * 0.15f,
            height * 0.72f,
            Math.min(width, height) * 0.12f
        )
        shootJoystick = Joystick(
            width * 0.85f,
            height * 0.72f,
            Math.min(width, height) * 0.12f
        )

        running = true
        thread = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
        }
    }

    fun resume() {
        if (!running) {
            running = true
            thread = Thread(this).also { it.start() }
        }
    }

    fun pause() {
        running = false
    }

    override fun run() {
        while (running) {
            val now = System.currentTimeMillis()
            val dt = if (lastTime == 0L) 0L else now - lastTime
            lastTime = now

            update(dt)
            drawFrame()

            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun update(dt: Long) {
        // Движение игрока по левому джойстику
        playerX += moveJoystick.dx * playerSpeed
        playerY += moveJoystick.dy * playerSpeed

        // Не выходим за границы
        if (playerX < playerSize / 2) playerX = playerSize / 2
        if (playerX > width - playerSize / 2) playerX = width - playerSize / 2
        if (playerY < playerSize / 2) playerY = playerSize / 2
        if (playerY > height - playerSize / 2) playerY = height - playerSize / 2

        // Стрельба правым джойстиком (пока не сделано)
        // TODO: пули
    }

    private fun drawFrame() {
        val holder = holder
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                drawGame(canvas)
            }
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun drawGame(canvas: Canvas) {
        // Фон
        canvas.drawColor(Color.rgb(30, 30, 40))

        // Сетка пола (для атмосферы подземелья)
        paint.color = Color.rgb(50, 50, 65)
        paint.strokeWidth = 2f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            x += 100f
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += 100f
        }

        // Игрок (квадрат)
        paint.color = Color.rgb(80, 200, 120)
        canvas.drawRect(
            playerX - playerSize / 2,
            playerY - playerSize / 2,
            playerX + playerSize / 2,
            playerY + playerSize / 2,
            paint
        )

        // Глаза (для направления — пока просто точка)
        paint.color = Color.WHITE
        canvas.drawCircle(playerX, playerY, 6f, paint)

        // Пули
        for (b in bullets) {
            b.draw(canvas, paint)
        }

        // Джойстики
        moveJoystick.draw(canvas)
        shootJoystick.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Передаём джойстикам, если попали
        if (moveJoystick.handleTouch(event)) return true
        if (shootJoystick.handleTouch(event)) return true
        return true
    }
}
