package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class TrapType {
    SPIKES,   // шипы — урон
    PIT       // яма — телепорт в начало комнаты
}

class Trap(
    var x: Float,
    var y: Float,
    val type: TrapType
) {
    val size = 60f
    var triggered = false
    private var cooldown = 0L

    fun canTrigger(): Boolean {
        val now = System.currentTimeMillis()
        if (now - cooldown > 800) {
            cooldown = now
            return true
        }
        return false
    }

    fun draw(canvas: Canvas, paint: Paint) {
        when (type) {
            TrapType.SPIKES -> {
                // Тёмный квадрат
                paint.color = Color.rgb(60, 40, 40)
                canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)

                // Шипы (треугольники)
                paint.color = Color.rgb(180, 180, 200)
                val path = android.graphics.Path()
                for (i in 0 until 3) {
                    for (j in 0 until 3) {
                        val sx = x - size / 2 + 10f + i * 20f
                        val sy = y - size / 2 + 10f + j * 20f
                        path.reset()
                        path.moveTo(sx, sy + 12f)
                        path.lineTo(sx + 6f, sy)
                        path.lineTo(sx + 12f, sy + 12f)
                        path.close()
                        canvas.drawPath(path, paint)
                    }
                }
            }
            TrapType.PIT -> {
                // Тёмный круг — яма
                paint.color = Color.rgb(10, 10, 15)
                canvas.drawCircle(x, y, size / 2, paint)
                paint.color = Color.rgb(40, 40, 60)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawCircle(x, y, size / 2, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }
}
