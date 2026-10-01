package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Chest(
    var x: Float,
    var y: Float
) {
    val size = 70f
    var opened = false

    fun draw(canvas: Canvas, paint: Paint) {
        if (opened) {
            // Открытый — крышка откинута
            paint.color = Color.rgb(100, 60, 30)
            canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)
            paint.color = Color.rgb(60, 30, 10)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)
            paint.style = Paint.Style.FILL

            // Пусто внутри
            paint.color = Color.rgb(20, 10, 5)
            canvas.drawRect(x - size / 2 + 10f, y - size / 2 + 10f, x + size / 2 - 10f, y + size / 2 - 10f, paint)
        } else {
            // Закрытый сундук
            paint.color = Color.rgb(140, 90, 40)
            canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)

            // Крышка (верхняя половина светлее)
            paint.color = Color.rgb(170, 110, 50)
            canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y - size / 2 + 20f, paint)

            // Замок
            paint.color = Color.rgb(220, 190, 60)
            canvas.drawRect(x - 8f, y - 4f, x + 8f, y + 12f, paint)
            paint.color = Color.rgb(140, 120, 30)
            canvas.drawCircle(x, y + 4f, 4f, paint)

            // Обводка
            paint.color = Color.rgb(70, 40, 15)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
