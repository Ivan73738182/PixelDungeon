package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Wall(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
) {
    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = Color.rgb(90, 70, 60)
        canvas.drawRect(x, y, x + w, y + h, paint)

        paint.color = Color.rgb(50, 35, 30)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(x, y, x + w, y + h, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(70, 55, 45)
        paint.strokeWidth = 2f
        val step = h / 3f
        var yy = y + step
        while (yy < y + h) {
            canvas.drawLine(x, yy, x + w, yy, paint)
            yy += step
        }
    }
}
