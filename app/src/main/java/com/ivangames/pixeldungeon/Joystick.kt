package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent

class Joystick(
    val centerX: Float,
    val centerY: Float,
    val radius: Float
) {
    var knobX: Float = centerX
    var knobY: Float = centerY
    var dx: Float = 0f  // -1..1
    var dy: Float = 0f  // -1..1
    var active: Boolean = false

    private val bgPaint = Paint().apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        color = Color.argb(150, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val knobPaint = Paint().apply {
        color = Color.argb(200, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, bgPaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
        canvas.drawCircle(knobX, knobY, radius * 0.4f, knobPaint)
    }

    fun handleTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (dist(x, y, centerX, centerY) < radius * 1.5f) {
                    active = true
                    update(x, y)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (active) {
                    update(x, y)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (active) {
                    active = false
                    knobX = centerX
                    knobY = centerY
                    dx = 0f
                    dy = 0f
                    return true
                }
            }
        }
        return false
    }

    private fun update(x: Float, y: Float) {
        var vx = x - centerX
        var vy = y - centerY
        val len = Math.sqrt((vx * vx + vy * vy).toDouble()).toFloat()
        if (len > radius) {
            vx = vx / len * radius
            vy = vy / len * radius
        }
        knobX = centerX + vx
        knobY = centerY + vy
        dx = vx / radius
        dy = vy / radius
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
