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

    var shootTimer = 0L
    val shootInterval = 1500L

    fun update(playerX: Float, playerY: Float, walls: List<Wall>, enemies: List<Enemy>) {
        val dx = playerX - x
        val dy = playerY - y
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 5f) return

        val nx = dx / len
        val ny = dy / len

        val stepX = nx * speed
        val stepY = ny * speed

        // Двигаемся по X, если не упираемся в стену
        if (!collides(x + stepX, y, walls)) {
            x += stepX
        }
        // Двигаемся по Y
        if (!collides(x, y + stepY, walls)) {
            y += stepY
        }
    }

    private fun collides(nx: Float, ny: Float, walls: List<Wall>): Boolean {
        val half = size / 2f
        val left = nx - half
        val right = nx + half
        val top = ny - half
        val bottom = ny + half
        for (w in walls) {
            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) {
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
        // Тело
        paint.color = Color.rgb(200, 60, 60)
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)

        // Обводка
        paint.color = Color.rgb(120, 20, 20)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)
        paint.style = Paint.Style.FILL

        // Глаз
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, 5f, paint)

        // Полоска HP над врагом
        val barW = size
        val barH = 8f
        val barX = x - barW / 2
        val barY = y - size / 2 - 16f

        // Фон
        paint.color = Color.rgb(40, 20, 20)
        canvas.drawRect(barX, barY, barX + barW, barY + barH, paint)

        // Заполнение
        val hpPercent = hp.toFloat() / maxHp.toFloat()
        paint.color = Color.rgb(220, 60, 60)
        canvas.drawRect(barX, barY, barX + barW * hpPercent, barY + barH, paint)
    }
}
