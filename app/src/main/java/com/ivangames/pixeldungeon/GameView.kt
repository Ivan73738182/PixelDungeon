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

    // Джойстики — создадим сразу, чтобы не было NPE
    private var moveJoystick: Joystick? = null
    private var shootJoystick: Joystick? = null

    // Скорость игрока
    private val playerSpeed = 8f

    // Пули
    private val bullets = mutableListOf<Bullet>()

    private var lastTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            playerX = width / 2f
            playerY = height / 2f

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
        } catch (e: Throwable) {
            android.util.Log.e("PixelDungeon", "surfaceCreated crash", e)
        }
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
        if (!running && width > 0 && height > 0) {
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

            try {
                update(dt)
                drawFrame()
            } catch (e: Throwable) {
                android.util.Log.e("PixelDungeon", "loop crash", e)
                running = false
            }

            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun update(dt: Long) {
        val mj = moveJoystick ?: return
        playerX += mj.dx * playerSpeed
        playerY += mj.dy * playerSpeed

        if (playerX < playerSize / 2) playerX = playerSize / 2
        if (playerX > width - playerSize / 2) playerX = width - playerSize / 2
        if (playerY < playerSize / 2) playerY = playerSize / 2
        if (playerY > height - playerSize / 2) playerY = height - playerSize / 2
    }

    private fun drawFrame() {
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
        canvas.drawColor(Color.rgb(30, 30, 40))

        // Сетка
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

        // Игрок
        paint.color = Color.rgb(80, 200, 120)
        canvas.drawRect(
            playerX - playerSize / 2,
            playerY - playerSize / 2,
            playerX + playerSize / 2,
            playerY + playerSize / 2,
            paint
        )

        // Глаз
        paint.color = Color.WHITE
        canvas.drawCircle(playerX, playerY, 6f, paint)

        // Пули
        for (b in bullets) {
            b.draw(canvas, paint)
        }

        // Джойстики
        moveJoystick?.draw(canvas)
        shootJoystick?.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        moveJoystick?.handleTouch(event)
        shootJoystick?.handleTouch(event)
        return true
    }
}
