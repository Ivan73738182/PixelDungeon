package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Bullet(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var speed: Float = 18f
) {
    var alive = true
    private val radius = 8f

    fun update() {
        x += dx * speed
        y += dy * speed
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = Color.argb(80, 255, 220, 80)
        canvas.drawCircle(x, y, radius * 1.8f, paint)
        paint.color = Color.rgb(255, 240, 150)
        canvas.drawCircle(x, y, radius, paint)
    }
}
