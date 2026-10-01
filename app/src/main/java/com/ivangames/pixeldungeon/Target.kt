package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Target(
    var x: Float,
    var y: Float
) {
    val size = 70f
    var hits = 0

    fun draw(canvas: Canvas, paint: Paint) {
        // Внешний круг
        paint.color = Color.rgb(230, 230, 230)
        canvas.drawCircle(x, y, size / 2, paint)

        // Красный круг
        paint.color = Color.rgb(200, 50, 50)
        canvas.drawCircle(x, y, size / 2 * 0.75f, paint)

        // Белый центр
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawCircle(x, y, size / 2 * 0.4f, paint)

        // Красная точка
        paint.color = Color.rgb(180, 30, 30)
        canvas.drawCircle(x, y, size / 2 * 0.15f, paint)

        // Обводка
        paint.color = Color.rgb(80, 80, 80)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(x, y, size / 2, paint)
        paint.style = Paint.Style.FILL

        // Счёт попаданий
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("$hits", x, y - size / 2 - 10f, paint)
        paint.textAlign = Paint.Align.LEFT
    }
}
