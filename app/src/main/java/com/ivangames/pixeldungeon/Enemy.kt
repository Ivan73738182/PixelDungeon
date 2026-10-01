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

    // Дистанция, на которой враг останавливается и стреляет
    val preferredDistance = 280f

    // Допустимое отклонение от дистанции (чтобы не дёргался)
    val distanceTolerance = 40f

    var shootTimer = 0L
    val shootInterval = 1500L

    fun update(playerX: Float, playerY: Float, walls: List<Wall>, enemies: List<Enemy>) {
        val dx = playerX - x
        val dy = playerY - y
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 5f) return

        val nx = dx / len
        val ny = dy / len

        // Если ближе "идеальной" дистанции — отходим назад
        // Если дальше — идём вперёд
        // Если в зоне tolerance — стоим
        val moveDir: Float
        if (len < preferredDistance - distanceTolerance) {
            // Слишком близко — отходим
            moveDir = -1f
        } else if (len > preferredDistance + distanceTolerance) {
            // Слишком далеко — идём вперёд
            moveDir = 1f
        } else {
            // В идеальной зоне — стоим
            return
        }

        val stepX = nx * speed * moveDir
        val stepY = ny * speed * moveDir

        // По X
        if (!collidesWalls(x + stepX, y, walls) && !collidesEnemies(x + stepX, y, enemies)) {
            x += stepX
        } else {
            // Если не можем двигаться по X — пробуем скользить по Y
            if (!collidesWalls(x, y + stepY, walls) && !collidesEnemies(x, y + stepY, enemies)) {
                y += stepY
            }
        }
        // По Y
        if (!collidesWalls(x, y + stepY, walls) && !collidesEnemies(x, y + stepY, enemies)) {
            y += stepY
        } else {
            if (!collidesWalls(x + stepX, y, walls) && !collidesEnemies(x + stepX, y, enemies)) {
                x += stepX
            }
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
