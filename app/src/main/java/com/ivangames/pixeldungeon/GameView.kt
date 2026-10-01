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
    private val playerSize = 60f
    private val playerSpeed = 8f

    // Джойстики
    private var moveJoystick: Joystick? = null
    private var shootJoystick: Joystick? = null

    // Пули
    private val bullets = mutableListOf<Bullet>()

    // Кулдаун стрельбы
    private var lastShotTime = 0L
    private val shotCooldown = 220L

    // Стены
    private val walls = mutableListOf<Wall>()

    private var lastTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            // Стартовая позиция — слева, в свободной зоне
            playerX = width * 0.25f
            playerY = height * 0.5f

            moveJoystick = Joystick(
                width * 0.15f,
                height * 0.75f,
                Math.min(width, height) * 0.12f
            )
            shootJoystick = Joystick(
                width * 0.85f,
                height * 0.75f,
                Math.min(width, height) * 0.12f
            )

            buildLevel()

            running = true
            thread = Thread(this).also { it.start() }
        } catch (e: Throwable) {
            android.util.Log.e("PixelDungeon", "surfaceCreated crash", e)
        }
    }

    private fun buildLevel() {
        walls.clear()
        val tile = 120f
        val cols = (width / tile).toInt()
        val rows = (height / tile).toInt()

        // Границы по краям
        for (i in 0 until cols) {
            walls.add(Wall(i * tile, 0f, tile, tile))
            walls.add(Wall(i * tile, (rows - 1) * tile, tile, tile))
        }
        for (j in 0 until rows) {
            walls.add(Wall(0f, j * tile, tile, tile))
            walls.add(Wall((cols - 1) * tile, j * tile, tile, tile))
        }

        // Угловые препятствия — центр пустой
        walls.add(Wall(tile * 3, tile * 3, tile, tile))
        walls.add(Wall(width - tile * 4, tile * 3, tile, tile))
        walls.add(Wall(tile * 3, height - tile * 4, tile, tile))
        walls.add(Wall(width - tile * 4, height - tile * 4, tile, tile))
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
        updatePlayer()
        updateShooting()
        updateBullets()
    }

    private fun updatePlayer() {
        val mj = moveJoystick ?: return

        val newX = playerX + mj.dx * playerSpeed
        val newY = playerY + mj.dy * playerSpeed

        if (!collidesWithWalls(newX, playerY)) {
            playerX = newX
        }
        if (!collidesWithWalls(playerX, newY)) {
            playerY = newY
        }

        pushOutOfWalls()

        if (playerX < playerSize / 2) playerX = playerSize / 2
        if (playerX > width - playerSize / 2) playerX = width - playerSize / 2
        if (playerY < playerSize / 2) playerY = playerSize / 2
        if (playerY > height - playerSize / 2) playerY = height - playerSize / 2
    }

    private fun collidesWithWalls(x: Float, y: Float): Boolean {
        val half = playerSize / 2f
        val left = x - half
        val right = x + half
        val top = y - half
        val bottom = y + half
        for (w in walls) {
            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) {
                return true
            }
        }
        return false
    }

    private fun pushOutOfWalls() {
        val half = playerSize / 2f
        for (w in walls) {
            val left = playerX - half
            val right = playerX + half
            val top = playerY - half
            val bottom = playerY + half

            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) {
                val overlapLeft = right - w.x
                val overlapRight = (w.x + w.w) - left
                val overlapTop = bottom - w.y
                val overlapBottom = (w.y + w.h) - top

                val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)

                when (minOverlap) {
                    overlapLeft -> playerX = w.x - half
                    overlapRight -> playerX = w.x + w.w + half
                    overlapTop -> playerY = w.y - half
                    overlapBottom -> playerY = w.y + w.h + half
                }
            }
        }
    }

    private fun updateShooting() {
        val sj = shootJoystick ?: return
        if (sj.dx == 0f && sj.dy == 0f) return

        val now = System.currentTimeMillis()
        if (now - lastShotTime < shotCooldown) return
        lastShotTime = now

        val bullet = Bullet(
            playerX,
            playerY,
            sj.dx,
            sj.dy,
            speed = 18f
        )
        bullets.add(bullet)
    }

    private fun updateBullets() {
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.update()

            if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
                it.remove()
                continue
            }

            for (w in walls) {
                if (b.x > w.x && b.x < w.x + w.w && b.y > w.y && b.y < w.y + w.h) {
                    it.remove()
                    break
                }
            }
        }
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

        // Сетка пола
        paint.color = Color.rgb(45, 45, 60)
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

        // Стены
        for (w in walls) {
            w.draw(canvas, paint)
        }

        // Пули
        for (b in bullets) {
            b.draw(canvas, paint)
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
        paint.color = Color.WHITE
        canvas.drawCircle(playerX, playerY, 5f, paint)

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
