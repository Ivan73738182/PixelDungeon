package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Enemy(
    var x: Float,
    var y: Float
) {
    val size = 60f
    var hp = 3
    val maxHp = 3
    var speed = 2.2f

    // Дистанция ведения боя
    val preferredDistance = 280f
    val distanceTolerance = 60f

    // Стрельба медленнее
    var shootTimer = 0L
    var shootInterval = 2500L

    // Стейф (движение вбок)
    private var strafeDir = if (Math.random() < 0.5) -1f else 1f
    private var strafeTimer = 0L
    private val strafeDuration = 1500L

    // Уворот
    private var dodgeTimer = 0L
    private var dodgeDirX = 0f
    private var dodgeDirY = 0f
    private val dodgeDuration = 400L
    private var lastDodgeCheck = 0L

    fun update(
        playerX: Float,
        playerY: Float,
        walls: List<Wall>,
        enemies: List<Enemy>,
        bullets: List<Bullet>
    ) {
        val dx = playerX - x
        val dy = playerY - y
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 5f) return

        val nx = dx / len
        val ny = dy / len

        val now = System.currentTimeMillis()

        // === УВОРОТ ОТ ПУЛЬ ===
        if (now - dodgeTimer > dodgeDuration) {
            // Проверяем пули игрока рядом
            for (b in bullets) {
                if (b.isEnemy) continue
                val bdx = b.x - x
                val bdy = b.y - y
                val blen = Math.sqrt((bdx * bdx + bdy * bdy).toDouble()).toFloat()
                // Пуля близко и летит примерно в нашу сторону
                if (blen < 200f) {
                    val dot = (b.dx * bdx + b.dy * bdy) / (blen + 0.01f)
                    if (dot > 0.5f) {
                        // Уворачиваемся перпендикулярно направлению пули
                        dodgeDirX = -b.dy
                        dodgeDirY = b.dx
                        // Случайно выбираем сторону
                        if (Math.random() < 0.5) {
                            dodgeDirX = -dodgeDirX
                            dodgeDirY = -dodgeDirY
                        }
                        dodgeTimer = now
                        break
                    }
                }
            }
        }

        val moveX: Float
        val moveY: Float

        if (now - dodgeTimer < dodgeDuration) {
            // Активный уворот
            moveX = dodgeDirX * speed * 1.6f
            moveY = dodgeDirY * speed * 1.6f
        } else {
            // Обычное поведение: держим дистанцию + стрейф
            val distStepX: Float
            val distStepY: Float

            if (len < preferredDistance - distanceTolerance) {
                distStepX = -nx * speed
                distStepY = -ny * speed
            } else if (len > preferredDistance + distanceTolerance) {
                distStepX = nx * speed
                distStepY = ny * speed
            } else {
                distStepX = 0f
                distStepY = 0f
            }

            // Меняем направление стрейфа время от времени
            if (now - strafeTimer > strafeDuration) {
                strafeTimer = now
                strafeDir = -strafeDir
            }

            // Стрейф — движение перпендикулярно игроку
            val strafeX = -ny * speed * 0.6f * strafeDir
            val strafeY = nx * speed * 0.6f * strafeDir

            moveX = distStepX + strafeX
            moveY = distStepY + strafeY
        }

        // Пробуем сдвинуться, учитывая стены и других врагов
        val tryX = x + moveX
        val tryY = y + moveY

        if (!collidesWalls(tryX, y, walls) && !collidesEnemies(tryX, y, enemies)) {
            x = tryX
        } else if (!collidesWalls(x, tryY, walls) && !collidesEnemies(x, tryY, enemies)) {
            y = tryY
        }
    }

    private fun collidesWalls(nx: Float, ny: Float, walls: List<Wall>): Boolean {
        val half = size / 2f
        val left = nx - half
        val right = nx + half
        val top = ny - half
        val bottom = ny + half
        for (w in walls) {
            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) return true
        }
        return false
    }

    private fun collidesEnemies(nx: Float, ny: Float, enemies: List<Enemy>): Boolean {
        val half = size / 2f
        val left = nx - half
        val right = nx + half
        val top = ny - half
        val bottom = ny + half
        for (e in enemies) {
            if (e === this) continue
            val ehalf = e.size / 2f
            if (right > e.x - ehalf && left < e.x + ehalf && bottom > e.y - ehalf && top < e.y + ehalf) {
                return true
            }
        }
        return false
    }

    fun canShoot(): Boolean {
        val now = System.currentTimeMillis()
        if (now - shootTimer > shootInterval) {
            shootTimer = now
            return true
        }
        return false
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = Color.rgb(200, 60, 60)
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)

        paint.color = Color.rgb(120, 20, 20)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.WHITE
        canvas.drawCircle(x, y, 5f, paint)

        // Полоска HP
        val barW = size
        val barH = 8f
        val barX = x - barW / 2
        val barY = y - size / 2 - 16f

        paint.color = Color.rgb(40, 20, 20)
        canvas.drawRect(barX, barY, barX + barW, barY + barH, paint)

        val hpPercent = hp.toFloat() / maxHp.toFloat()
        paint.color = Color.rgb(220, 60, 60)
        canvas.drawRect(barX, barY, barX + barW * hpPercent, barY + barH, paint)
    }
}
