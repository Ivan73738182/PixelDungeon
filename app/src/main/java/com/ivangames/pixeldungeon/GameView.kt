package com.ivangames.pixeldungeon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

    // HP и мана
    private var playerHp = 5
    private val playerMaxHp = 5
    private var playerMana = 100
    private val playerMaxMana = 100

    // Джойстики
    private var moveJoystick: Joystick? = null
    private var shootJoystick: Joystick? = null

    // Пули игрока и врагов
    private val bullets = mutableListOf<Bullet>()
    private val enemyBullets = mutableListOf<Bullet>()

    // Кулдаун стрельбы
    private var lastShotTime = 0L
    private val shotCooldown = 220L

    // Стены
    private val walls = mutableListOf<Wall>()

    // Враги
    private val enemies = mutableListOf<Enemy>()

    private var lastTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
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
            spawnEnemies()
            findFreeStart()

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

        // Только границы по краям — внутренних препятствий нет
        for (i in 0 until cols) {
            walls.add(Wall(i * tile, 0f, tile, tile))
            walls.add(Wall(i * tile, (rows - 1) * tile, tile, tile))
        }
        for (j in 0 until rows) {
            walls.add(Wall(0f, j * tile, tile, tile))
            walls.add(Wall((cols - 1) * tile, j * tile, tile, tile))
        }
    }

    private fun spawnEnemies() {
        enemies.clear()
        // Спавним 4 врага в правой половине, подальше от игрока
        enemies.add(Enemy(width * 0.65f, height * 0.3f))
        enemies.add(Enemy(width * 0.75f, height * 0.5f))
        enemies.add(Enemy(width * 0.65f, height * 0.7f))
        enemies.add(Enemy(width * 0.85f, height * 0.4f))
    }

    private fun findFreeStart() {
        if (!collidesWithWalls(playerX, playerY)) return

        val step = 60f
        var yy = step * 2
        while (yy < height - step * 2) {
            var xx = step * 2
            while (xx < width - step * 2) {
                if (!collidesWithWalls(xx, yy)) {
                    playerX = xx
                    playerY = yy
                    return
                }
                xx += step
            }
            yy += step
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
        updatePlayer()
        updateShooting()
        updateBullets()
        updateEnemies()
    }

    private fun updatePlayer() {
        val mj = moveJoystick ?: return

        val newX = playerX + mj.dx * playerSpeed
        val newY = playerY + mj.dy * playerSpeed

        if (!collidesWithWalls(newX, playerY)) playerX = newX
        if (!collidesWithWalls(playerX, newY)) playerY = newY

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
            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) return true
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

        // Проверяем ману
        if (playerMana <= 0) return

        lastShotTime = now
        playerMana -= 1

        val bullet = Bullet(playerX, playerY, sj.dx, sj.dy, speed = 18f)
        bullet.isEnemy = false
        bullets.add(bullet)
    }

    private fun updateBullets() {
        // Пули игрока
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.update()

            if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
                it.remove()
                continue
            }

            // Стены
            var removed = false
            for (w in walls) {
                if (b.x > w.x && b.x < w.x + w.w && b.y > w.y && b.y < w.y + w.h) {
                    it.remove()
                    removed = true
                    break
                }
            }
            if (removed) continue

            // Попадание во врага
            for (e in enemies) {
                if (b.x > e.x - e.size / 2 && b.x < e.x + e.size / 2 &&
                    b.y > e.y - e.size / 2 && b.y < e.y + e.size / 2) {
                    e.hp -= 1
                    it.remove()
                    break
                }
            }
        }

        // Пули врагов
        val it2 = enemyBullets.iterator()
        while (it2.hasNext()) {
            val b = it2.next()
            b.update()

            if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
                it2.remove()
                continue
            }

            for (w in walls) {
                if (b.x > w.x && b.x < w.x + w.w && b.y > w.y && b.y < w.y + w.h) {
                    it2.remove()
                    break
                }
            }
        }
    }

    private fun updateEnemies() {
        // Убираем мёртвых
        enemies.removeAll { it.hp <= 0 }

        for (e in enemies) {
            e.update(playerX, playerY, walls, enemies)

            // Стрельба по игроку
            if (e.canShoot()) {
                val dx = playerX - e.x
                val dy = playerY - e.y
                val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (len > 1f) {
                    val bullet = Bullet(e.x, e.y, dx / len, dy / len, speed = 10f)
                    bullet.isEnemy = true
                    enemyBullets.add(bullet)
                }
            }

            // Контакт с игроком — урон
            val dx = playerX - e.x
            val dy = playerY - e.y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist < (playerSize + e.size) / 2f) {
                // Урон по игроку
                if (System.currentTimeMillis() - lastPlayerHit > 800) {
                    lastPlayerHit = System.currentTimeMillis()
                    playerHp -= 1
                }
            }
        }

        // Пули врагов — попадание в игрока
        val it = enemyBullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            if (b.x > playerX - playerSize / 2 && b.x < playerX + playerSize / 2 &&
                b.y > playerY - playerSize / 2 && b.y < playerY + playerSize / 2) {
                if (System.currentTimeMillis() - lastPlayerHit > 500) {
                    lastPlayerHit = System.currentTimeMillis()
                    playerHp -= 1
                }
                it.remove()
            }
        }
    }

    private var lastPlayerHit = 0L

    private fun drawFrame() {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) drawGame(canvas)
        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(Color.rgb(30, 30, 40))

        // Сетка
        paint.color = Color.rgb(45, 45, 60)
        paint.strokeWidth = 2f
        var x = 0f
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += 100f }
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += 100f }

        // Стены
        for (w in walls) w.draw(canvas, paint)

        // Враги
        for (e in enemies) e.draw(canvas, paint)

        // Пули
        for (b in bullets) b.draw(canvas, paint)
        for (b in enemyBullets) b.draw(canvas, paint)

        // Игрок
        paint.color = Color.rgb(80, 200, 120)
        canvas.drawRect(
            playerX - playerSize / 2, playerY - playerSize / 2,
            playerX + playerSize / 2, playerY + playerSize / 2, paint
        )
        paint.color = Color.WHITE
        canvas.drawCircle(playerX, playerY, 5f, paint)

        // HUD
        drawHud(canvas)

        // Джойстики
        moveJoystick?.draw(canvas)
        shootJoystick?.draw(canvas)
    }

    private fun drawHud(canvas: Canvas) {
        val margin = 30f
        val heartSize = 40f
        val heartGap = 12f

        // Сердечки (HP) — сверху слева
        for (i in 0 until playerMaxHp) {
            val hx = margin + i * (heartSize + heartGap)
            val hy = margin
            if (i < playerHp) {
                paint.color = Color.rgb(230, 50, 80)
            } else {
                paint.color = Color.rgb(70, 40, 45)
            }
            drawHeart(canvas, hx, hy, heartSize)
        }

        // Полоска маны — под сердечками
        val barX = margin
        val barY = margin + heartSize + 20f
        val barW = 300f
        val barH = 22f

        // Фон
        paint.color = Color.rgb(20, 20, 50)
        val bgRect = RectF(barX, barY, barX + barW, barY + barH)
        canvas.drawRoundRect(bgRect, 8f, 8f, paint)

        // Заполнение
        val manaPercent = playerMana.toFloat() / playerMaxMana.toFloat()
        paint.color = Color.rgb(70, 130, 240)
        val fillRect = RectF(barX, barY, barX + barW * manaPercent, barY + barH)
        canvas.drawRoundRect(fillRect, 8f, 8f, paint)

        // Обводка
        paint.color = Color.rgb(120, 170, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(bgRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Текст
        paint.color = Color.WHITE
        paint.textSize = 26f
        canvas.drawText("$playerMana / $playerMaxMana", barX + barW + 20f, barY + barH - 4f, paint)
    }

    private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float) {
        // Рисуем сердце из двух кругов и треугольника
        val r = size / 4f
        canvas.drawCircle(x + r, y + r, r, paint)
        canvas.drawCircle(x + 3 * r, y + r, r, paint)

        val path = android.graphics.Path()
        path.moveTo(x, y + r)
        path.lineTo(x + size, y + r)
        path.lineTo(x + size / 2, y + size)
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        moveJoystick?.handleTouch(event)
        shootJoystick?.handleTouch(event)
        return true
    }
}
