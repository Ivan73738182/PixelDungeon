package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class EnemyType {
    NORMAL, FAST, TANK, SHOOTER, BOSS
}

class Enemy(
    var x: Float,
    var y: Float,
    val type: EnemyType = EnemyType.NORMAL
) {
    val size: Float
    var hp: Int
    val maxHp: Int
    var speed: Float

    val preferredDistance: Float
    val distanceTolerance = 60f

    var shootTimer = 0L
    var shootInterval: Long

    var xpReward: Int
    var damage = 1

    // Цвета по типу
    private val bodyColor: Int
    private val borderColor: Int

    private var strafeDir = if (Math.random() < 0.5) -1f else 1f
    private var strafeTimer = 0L
    private val strafeDuration = 1500L

    private var dodgeTimer = 0L
    private var dodgeDirX = 0f
    private var dodgeDirY = 0f
    private val dodgeDuration = 400L

    init {
        when (type) {
            EnemyType.NORMAL -> {
                size = 60f; hp = 3; maxHp = 3; speed = 2.2f
                shootInterval = 2500L; xpReward = 10
                preferredDistance = 280f
                bodyColor = Color.rgb(200, 60, 60)
                borderColor = Color.rgb(120, 20, 20)
            }
            EnemyType.FAST -> {
                size = 50f; hp = 2; maxHp = 2; speed = 4.0f
                shootInterval = 3000L; xpReward = 15
                preferredDistance = 220f
                bodyColor = Color.rgb(240, 200, 60)
                borderColor = Color.rgb(160, 120, 20)
            }
            EnemyType.TANK -> {
                size = 90f; hp = 8; maxHp = 8; speed = 1.4f
                shootInterval = 3200L; xpReward = 25
                preferredDistance = 320f
                bodyColor = Color.rgb(120, 120, 140)
                borderColor = Color.rgb(70, 70, 90)
            }
            EnemyType.SHOOTER -> {
                size = 55f; hp = 2; maxHp = 2; speed = 2.0f
                shootInterval = 1400L; xpReward = 20
                preferredDistance = 380f
                bodyColor = Color.rgb(90, 130, 240)
                borderColor = Color.rgb(40, 70, 180)
            }
            EnemyType.BOSS -> {
                size = 160f; hp = 30; maxHp = 30; speed = 1.8f
                shootInterval = 1200L; xpReward = 100
                preferredDistance = 320f
                bodyColor = Color.rgb(180, 40, 180)
                borderColor = Color.rgb(90, 10, 90)
                damage = 2
            }
        }
    }

    fun update(
        playerX: Float,
        playerY: Float,
        walls: List<Wall>,
        enemies: List<Enemy>,
        bullets: List<Bullet>
    ) {
        val dx = playerX - x
        val dy = playerY - y
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 5f) return

        val nx = dx / len
        val ny = dy / len

        val now = System.currentTimeMillis()

        // Уворот от пуль игрока (не для босса — он большой)
        if (type != EnemyType.BOSS && now - dodgeTimer > dodgeDuration) {
            for (b in bullets) {
                if (b.isEnemy) continue
                val bdx = b.x - x
                val bdy = b.y - y
                val blen = Math.sqrt((bdx * bdx + bdy * bdy).toDouble()).toFloat()
                if (blen < 200f) {
                    val dot = (b.dx * bdx + b.dy * bdy) / (blen + 0.01f)
                    if (dot > 0.5f) {
                        dodgeDirX = -b.dy
                        dodgeDirY = b.dx
                        if (Math.random() < 0.5) {
                            dodgeDirX = -dodgeDirX
                            dodgeDirY = -dodgeDirY
                        }
                        dodgeTimer = now
                        break
                    }
                }
            }
        }

        val moveX: Float
        val moveY: Float

        if (now - dodgeTimer < dodgeDuration) {
            moveX = dodgeDirX * speed * 1.6f
            moveY = dodgeDirY * speed * 1.6f
        } else {
            val distStepX: Float
            val distStepY: Float

            if (len < preferredDistance - distanceTolerance) {
                distStepX = -nx * speed
                distStepY = -ny * speed
            } else if (len > preferredDistance + distanceTolerance) {
                distStepX = nx * speed
                distStepY = ny * speed
            } else {
                distStepX = 0f
                distStepY = 0f
            }

            if (now - strafeTimer > strafeDuration) {
                strafeTimer = now
                strafeDir = -strafeDir
            }

            val strafeX = -ny * speed * 0.6f * strafeDir
            val strafeY = nx * speed * 0.6f * strafeDir

            moveX = distStepX + strafeX
            moveY = distStepY + strafeY
        }

        val tryX = x + moveX
        val tryY = y + moveY

        if (!collidesWalls(tryX, y, walls) && !collidesEnemies(tryX, y, enemies)) {
            x = tryX
        } else if (!collidesWalls(x, tryY, walls) && !collidesEnemies(x, tryY, enemies)) {
            y = tryY
        }

        // ФИКС: выталкивание из стен, если всё-таки застрял
        pushOutOfWalls(walls)
        pushOutOfEnemies(enemies)
    }

    private fun pushOutOfWalls(walls: List<Wall>) {
        val half = size / 2f
        for (w in walls) {
            val left = x - half
            val right = x + half
            val top = y - half
            val bottom = y + half

            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) {
                val overlapLeft = right - w.x
                val overlapRight = (w.x + w.w) - left
                val overlapTop = bottom - w.y
                val overlapBottom = (w.y + w.h) - top
                val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                when (minOverlap) {
                    overlapLeft -> x = w.x - half
                    overlapRight -> x = w.x + w.w + half
                    overlapTop -> y = w.y - half
                    overlapBottom -> y = w.y + w.h + half
                }
            }
        }
    }

    private fun pushOutOfEnemies(enemies: List<Enemy>) {
        val half = size / 2f
        for (e in enemies) {
            if (e === this) continue
            val ehalf = e.size / 2f
            val left = x - half
            val right = x + half
            val top = y - half
            val bottom = y + half

            if (right > e.x - ehalf && left < e.x + ehalf && bottom > e.y - ehalf && top < e.y + ehalf) {
                val overlapLeft = right - (e.x - ehalf)
                val overlapRight = (e.x + ehalf) - left
                val overlapTop = bottom - (e.y - ehalf)
                val overlapBottom = (e.y + ehalf) - top
                val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                when (minOverlap) {
                    overlapLeft -> x = e.x - ehalf - half
                    overlapRight -> x = e.x + ehalf + half
                    overlapTop -> y = e.y - ehalf - half
                    overlapBottom -> y = e.y + ehalf + half
                }
            }
        }
    }

    private fun collidesWalls(nx: Float, ny: Float, walls: List<Wall>): Boolean {
        val half = size / 2f
        val left = nx - half
        val right = nx + half
        val top = ny - half
        val bottom = ny + half
        for (w in walls) {
            if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) return true
        }
        return false
    }

    private fun collidesEnemies(nx: Float, ny: Float, enemies: List<Enemy>): Boolean {
        val half = size / 2f
        val left = nx - half
        val right = nx + half
        val top = ny - half
        val bottom = ny + half
        for (e in enemies) {
            if (e === this) continue
            val ehalf = e.size / 2f
            if (right > e.x - ehalf && left < e.x + ehalf && bottom > e.y - ehalf && top < e.y + ehalf) {
                return true
            }
        }
        return false
    }

    fun canShoot(): Boolean {
        val now = System.currentTimeMillis()
        if (now - shootTimer > shootInterval) {
            shootTimer = now
            return true
        }
        return false
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = bodyColor
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)

        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (type == EnemyType.BOSS) 8f else 4f
        canvas.drawRect(x - size / 2, y - size / 2, x + size / 2, y + size / 2, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.WHITE
        canvas.drawCircle(x, y, if (type == EnemyType.BOSS) 12f else 5f, paint)

        // Полоска HP
        val barW = size
        val barH = if (type == EnemyType.BOSS) 14f else 8f
        val barX = x - barW / 2
        val barY = y - size / 2 - (barH + 10f)

        paint.color = Color.rgb(40, 20, 20)
        canvas.drawRect(barX, barY, barX + barW, barY + barH, paint)

        val hpPercent = hp.toFloat() / maxHp.toFloat()
        paint.color = bodyColor
        canvas.drawRect(barX, barY, barX + barW * hpPercent, barY + barH, paint)
    }
}
