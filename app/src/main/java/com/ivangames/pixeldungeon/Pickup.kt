package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class PickupType {
    COIN, HEAL, MANA
}

class Pickup(
    var x: Float,
    var y: Float,
    val type: PickupType
) {
    val radius = 18f
    var alive = true
    private var bobTime = 0f

    fun update() {
        bobTime += 0.15f
    }

    fun getBobOffset(): Float {
        return Math.sin(bobTime.toDouble()).toFloat() * 5f
    }

    fun draw(canvas: Canvas, paint: Paint) {
        val oy = y + getBobOffset()

        when (type) {
            PickupType.COIN -> {
                // Монета — жёлтый круг с обводкой
                paint.color = Color.rgb(255, 200, 40)
                canvas.drawCircle(x, oy, radius, paint)
                paint.color = Color.rgb(180, 130, 20)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawCircle(x, oy, radius, paint)
                paint.style = Paint.Style.FILL
                // Внутренний кружок
                paint.color = Color.rgb(255, 230, 120)
                canvas.drawCircle(x, oy, radius * 0.5f, paint)
            }

            PickupType.HEAL -> {
                // Аптечка — красный квадрат с белым крестом
                paint.color = Color.rgb(230, 60, 80)
                canvas.drawRect(x - radius, oy - radius, x + radius, oy + radius, paint)
                paint.color = Color.WHITE
                canvas.drawRect(x - 4f, oy - radius * 0.6f, x + 4f, oy + radius * 0.6f, paint)
                canvas.drawRect(x - radius * 0.6f, oy - 4f, x + radius * 0.6f, oy + 4f, paint)
            }

            PickupType.MANA -> {
                // Зелье маны — синий круг
                paint.color = Color.rgb(60, 120, 240)
                canvas.drawCircle(x, oy, radius, paint)
                paint.color = Color.rgb(150, 200, 255)
                canvas.drawCircle(x - 4f, oy - 4f, radius * 0.35f, paint)
            }
        }
    }
}
