package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class DoorSide { LEFT, RIGHT, TOP, BOTTOM }

class Door(
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    val side: DoorSide,
    val targetRoomIndex: Int
) {
    var isOpen = false
    var isLocked = false

    fun draw(canvas: Canvas, paint: Paint) {
        if (isLocked) {
            // Закрытая дверь — красная решётка
            paint.color = Color.rgb(140, 40, 40)
            canvas.drawRect(x, y, x + w, y + h, paint)

            // Решётка
            paint.color = Color.rgb(90, 20, 20)
            paint.strokeWidth = 6f
            if (side == DoorSide.LEFT || side == DoorSide.RIGHT) {
                var yy = y + 10f
                while (yy < y + h - 10f) {
                    canvas.drawLine(x, yy, x + w, yy, paint)
                    yy += 20f
                }
            } else {
                var xx = x + 10f
                while (xx < x + w - 10f) {
                    canvas.drawLine(xx, y, xx, y + h, paint)
                    xx += 20f
                }
            }
        } else if (isOpen) {
            // Открытая дверь — зелёный проход (или пусто)
            paint.color = Color.argb(100, 80, 220, 120)
            canvas.drawRect(x, y, x + w, y + h, paint)
        }
    }

    fun contains(px: Float, py: Float): Boolean {
        return px > x && px < x + w && py > y && py < y + h
    }
}
