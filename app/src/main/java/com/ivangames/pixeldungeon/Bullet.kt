package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Bullet(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var speed: Float = 15f
) {
    var alive = true
    private val radius = 8f

    fun update() {
        x += dx * speed
        y += dy * speed
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = Color.rgb(255, 220, 80)
        canvas.drawCircle(x, y, radius, paint)
    }
}
