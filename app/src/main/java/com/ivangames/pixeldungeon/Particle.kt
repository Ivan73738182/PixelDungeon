package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Particle(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var color: Int,
    var life: Int = 20
) {
    var alive = true

    fun update() {
        x += dx
        y += dy
        dx *= 0.95f
        dy *= 0.95f
        life--
        if (life <= 0) alive = false
    }

    fun draw(canvas: Canvas, paint: Paint) {
        val alpha = (life * 255 / 20).coerceIn(0, 255)
        paint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(x, y, 5f, paint)
    }
}
