package com.ivangames.pixeldungeon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var running = false

    private val paint = Paint().apply { isAntiAlias = true }

    // Игрок
    private var playerX = 0f
    private var playerY = 0f
    private val playerSize = 60f
    private var playerSpeed = 8f

    // HP и мана
    private var playerHp = 5
    private var playerMaxHp = 5
    private var playerMana = 100
    private val playerMaxMana = 100

    // Опыт и уровень
    private var playerXp = 0
    private var playerLevel = 1
    private var xpToNextLevel = 30

    // Счёт
    private var coins = 0

    // Джойстики
    private var moveJoystick: Joystick? = null
    private var shootJoystick: Joystick? = null

    // Пули
    private val bullets = mutableListOf<Bullet>()
    private val enemyBullets = mutableListOf<Bullet>()

    private var lastShotTime = 0L
    private val shotCooldown = 220L

    // Стены
    private val walls = mutableListOf<Wall>()

    // Враги, лут, частицы
    private val enemies = mutableListOf<Enemy>()
    private val pickups = mutableListOf<Pickup>()
    private val particles = mutableListOf<Particle>()

    // Комнаты
    private val rooms = mutableListOf<Room>()
    private var currentRoomIndex = 0
    private val allDoors = mutableListOf<Door>()

    // Ожидание между комнатами (пауза перед переходом)
    private var roomCleared = false

    // Состояние игры
    private var gameState = GameState.PLAYING
    enum class GameState { PLAYING, GAME_OVER, VICTORY }

    private var lastTime = 0L
    private var lastPlayerHit = 0L

    private var movePointerId = -1
    private var shootPointerId = -1

    // Размеры комнаты
    private var roomLeft = 0f
    private var roomTop = 0f
    private var roomRight = 0f
    private var roomBottom = 0f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            moveJoystick = Joystick(
                width * 0.15f,
                height * 0.75f,
                Math.min(width, height) * 0.12f
            )
            shootJoystick = Joystick(
                width * 0.85f,
                height * 0.75f,
                Math.min(width, height) * 0.12f
            )

            generateLevel()
            buildRoomWalls()
            loadRoom(0)

            running = true
            thread = Thread(this).also { it.start() }
        } catch (e: Throwable) {
            android.util.Log.e("PixelDungeon", "surfaceCreated crash", e)
        }
    }

    // Генерация 3-6 комнат разной сложности
    private fun generateLevel() {
        rooms.clear()
        allDoors.clear()

        val roomCount = 3 + (Math.random() * 4).toInt() // 3..6
        for (i in 0 until roomCount) {
            val isLast = (i == roomCount - 1)
            val enemyCount: Int
            val enemyType: Int
            if (isLast) {
                // Последняя комната — босс
                enemyCount = 1
                enemyType = 4
            } else if (i == 0) {
                enemyCount = 3
                enemyType = 0
            } else if (i == 1) {
                enemyCount = 3
                enemyType = 1
            } else if (i == 2) {
                enemyCount = 2
                enemyType = 2
            } else {
                enemyCount = 3 + (Math.random() * 2).toInt()
                enemyType = (Math.random() * 4).toInt()
            }
            rooms.add(Room(i, enemyCount, enemyType))
        }
    }

    // Строим стены вокруг комнаты и двери
    private fun buildRoomWalls() {
        walls.clear()
        val tile = 120f
        val cols = (width / tile).toInt()
        val rows = (height / tile).toInt()

        // Область комнаты — внутренняя часть
        roomLeft = tile
        roomTop = tile
        roomRight = (cols - 1) * tile
        roomBottom = (rows - 1) * tile

        // Границы
        for (i in 0 until cols) {
            walls.add(Wall(i * tile, 0f, tile, tile))
            walls.add(Wall(i * tile, (rows - 1) * tile, tile, tile))
        }
        for (j in 0 until rows) {
            walls.add(Wall(0f, j * tile, tile, tile))
            walls.add(Wall((cols - 1) * tile, j * tile, tile, tile))
        }

        // Двери — по 2-4 штуки в зависимости от числа комнат
        allDoors.clear()
        val doorSize = tile
        val midY = height / 2f - doorSize / 2f
        val midX = width / 2f - doorSize / 2f

        // Левая дверь → предыдущая комната
        if (rooms.size >= 2) {
            allDoors.add(
                Door(0f, midY, tile, doorSize, DoorSide.LEFT, -1)
            )
        }
        // Правая дверь → следующая комната
        if (rooms.size >= 2) {
            allDoors.add(
                Door(width - tile, midY, tile, doorSize, DoorSide.RIGHT, 1)
            )
        }
    }

    // Загружаем комнату по индексу
    private fun loadRoom(index: Int) {
        currentRoomIndex = index
        val room = rooms[index]

        // Очистка
        enemies.clear()
        bullets.clear()
        enemyBullets.clear()
        pickups.clear()
        particles.clear()

        roomCleared = false

        // Игрок — с левой стороны, если пришли слева
        playerX = roomLeft + playerSize + 40f
        playerY = (roomTop + roomBottom) / 2f

        // Спавн врагов в правой/центральной части
        val total = room.enemyCount
        for (i in 0 until total) {
            val ex: Float
            val ey: Float
            if (room.enemyType == 4) {
                // Босс — в центре правой части
                ex = roomRight - 250f
                ey = (roomTop + roomBottom) / 2f
            } else {
                ex = width * (0.5f + Math.random().toFloat() * 0.4f)
                ey = height * (0.2f + Math.random().toFloat() * 0.6f)
            }
            val type = when (room.enemyType) {
                0 -> EnemyType.NORMAL
                1 -> EnemyType.FAST
                2 -> EnemyType.TANK
                3 -> EnemyType.SHOOTER
                else -> EnemyType.BOSS
            }
            enemies.add(Enemy(ex, ey, type))
        }

        // Двери: закрыты, пока враги живы
        updateDoorsLock()
    }

    private fun updateDoorsLock() {
        val locked = enemies.isNotEmpty()
        for (d in allDoors) {
            d.isLocked = locked
            d.isOpen = !locked
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
        }
    }

    fun resume() {
        if (!running && width > 0 && height > 0) {
            running = true
            thread = Thread(this).also { it.start() }
        }
    }

    fun pause() {
        running = false
    }

    override fun run() {
        while (running) {
            val now = System.currentTimeMillis()
            val dt = if (lastTime == 0L) 0L else now - lastTime
            lastTime = now

            try {
                update(dt)
                drawFrame()
            } catch (e: Throwable) {
                android.util.Log.e("PixelDungeon", "loop crash", e)
                running = false
            }

            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun update(dt: Long) {
        if (gameState != GameState.PLAYING) return

        updatePlayer()
        updateShooting()
        updateBullets()
        updateEnemies()
        updatePickups()
        updateParticles()
        checkRoomClear()
        checkDoorsTransition()
    }

    private fun checkRoomClear() {
        if (!roomCleared && enemies.isEmpty()) {
            roomCleared = true
            rooms[currentRoomIndex].cleared = true
            updateDoorsLock()

            // Последняя комната — победа
            if (currentRoomIndex == rooms.size - 1) {
                gameState = GameState.VICTORY
            }
        }
    }

    private fun checkDoorsTransition() {
        if (!roomCleared) return
        for (d in allDoors) {
            if (d.isOpen && d.contains(playerX, playerY)) {
                var nextIndex = currentRoomIndex + d.targetRoomIndex
                if (d.side == DoorSide.LEFT) nextIndex = currentRoomIndex - 1
                if (d.side == DoorSide.RIGHT) nextIndex = currentRoomIndex + 1

                if (nextIndex in 0 until rooms.size) {
                    loadRoom(nextIndex)
                    // Смещаем игрока внутрь комнаты (чтобы не застрял в двери)
                    if (d.side == DoorSide.LEFT) {
                        playerX = roomLeft + playerSize + 40f
                    } else if (d.side == DoorSide.RIGHT) {
                        playerX = roomRight - playerSize - 40f
                    }
                    return
                }
            }
        }
    }

    private fun spawnParticles(x: Float, y: Float, color: Int, count: Int = 6) {
        for (i in 0 until count) {
            val angle = Math.random() * Math.PI * 2
            val sp = 2f + Math.random().toFloat() * 3f
            particles.add(Particle(
                x, y,
                Math.cos(angle).toFloat() * sp,
                Math.sin(angle).toFloat() * sp,
                color
            ))
        }
    }

    private fun dropLoot(x: Float, y: Float) {
        val r = Math.random()
        when {
            r < 0.55f -> pickups.add(Pickup(x, y, PickupType.COIN))
            r < 0.80f -> pickups.add(Pickup(x, y, PickupType.HEAL))
            else -> pickups.add(Pickup(x, y, PickupType.MANA))
        }
    }

    private fun updatePickups() {
        for (p in pickups) p.update()

        val it = pickups.iterator()
        while (it.hasNext()) {
            val p = it.next()
            val dx = playerX - p.x
            val dy = playerY - p.y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist < (playerSize / 2 + p.radius)) {
                when (p.type) {
                    PickupType.COIN -> coins += 1
                    PickupType.HEAL -> if (playerHp < playerMaxHp) playerHp += 1
                    PickupType.MANA -> playerMana = (playerMana + 30).coerceAtMost(playerMaxMana)
                }
                it.remove()
            }
        }
    }

    private fun updateParticles() {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.update()
            if (!p.alive) it.remove()
        }
    }

    private fun addXp(amount: Int) {
        playerXp += amount
        while (playerXp >= xpToNextLevel) {
            playerXp -= xpToNextLevel
            playerLevel++
            xpToNextLevel = (xpToNextLevel * 1.4f).toInt()
            if (playerLevel % 2 == 0) {
                playerMaxHp += 1
                playerHp = playerMaxHp
            } else {
                playerSpeed += 0.5f
            }
        }
    }
private fun updatePlayer() {
    val mj = moveJoystick ?: return

    val newX = playerX + mj.dx * playerSpeed
    val newY = playerY + mj.dy * playerSpeed

    if (!collidesWithWalls(newX, playerY) && !collidesWithEnemies(newX, playerY)) {
        playerX = newX
    }
    if (!collidesWithWalls(playerX, newY) && !collidesWithEnemies(playerX, newY)) {
        playerY = newY
    }

    pushOutOfWalls()
    pushOutOfEnemies()

    if (playerX < playerSize / 2) playerX = playerSize / 2
    if (playerX > width - playerSize / 2) playerX = width - playerSize / 2
    if (playerY < playerSize / 2) playerY = playerSize / 2
    if (playerY > height - playerSize / 2) playerY = height - playerSize / 2
}

private fun collidesWithWalls(x: Float, y: Float): Boolean {
    val half = playerSize / 2f
    val left = x - half
    val right = x + half
    val top = y - half
    val bottom = y + half
    for (w in walls) {
        if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) return true
    }
    return false
}

private fun collidesWithEnemies(x: Float, y: Float): Boolean {
    val half = playerSize / 2f
    val left = x - half
    val right = x + half
    val top = y - half
    val bottom = y + half
    for (e in enemies) {
        val ehalf = e.size / 2f
        if (right > e.x - ehalf && left < e.x + ehalf && bottom > e.y - ehalf && top < e.y + ehalf) {
            return true
        }
    }
    return false
}

private fun pushOutOfWalls() {
    val half = playerSize / 2f
    for (w in walls) {
        val left = playerX - half
        val right = playerX + half
        val top = playerY - half
        val bottom = playerY + half

        if (right > w.x && left < w.x + w.w && bottom > w.y && top < w.y + w.h) {
            val overlapLeft = right - w.x
            val overlapRight = (w.x + w.w) - left
            val overlapTop = bottom - w.y
            val overlapBottom = (w.y + w.h) - top
            val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
            when (minOverlap) {
                overlapLeft -> playerX = w.x - half
                overlapRight -> playerX = w.x + w.w + half
                overlapTop -> playerY = w.y - half
                overlapBottom -> playerY = w.y + w.h + half
            }
        }
    }
}

private fun pushOutOfEnemies() {
    val half = playerSize / 2f
    for (e in enemies) {
        val ehalf = e.size / 2f
        val left = playerX - half
        val right = playerX + half
        val top = playerY - half
        val bottom = playerY + half

        if (right > e.x - ehalf && left < e.x + ehalf && bottom > e.y - ehalf && top < e.y + ehalf) {
            val overlapLeft = right - (e.x - ehalf)
            val overlapRight = (e.x + ehalf) - left
            val overlapTop = bottom - (e.y - ehalf)
            val overlapBottom = (e.y + ehalf) - top
            val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
            when (minOverlap) {
                overlapLeft -> playerX = e.x - ehalf - half
                overlapRight -> playerX = e.x + ehalf + half
                overlapTop -> playerY = e.y - ehalf - half
                overlapBottom -> playerY = e.y + ehalf + half
            }
        }
    }
}

private fun updateShooting() {
    val sj = shootJoystick ?: return
    if (sj.dx == 0f && sj.dy == 0f) return

    val now = System.currentTimeMillis()
    if (now - lastShotTime < shotCooldown) return
    if (playerMana <= 0) return

    lastShotTime = now
    playerMana -= 1

    val bullet = Bullet(playerX, playerY, sj.dx, sj.dy, speed = 18f)
    bullet.isEnemy = false
    bullets.add(bullet)
}

private fun updateBullets() {
    val it = bullets.iterator()
    while (it.hasNext()) {
        val b = it.next()
        b.update()

        if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
            it.remove()
            continue
        }

        var removed = false
        for (w in walls) {
            if (b.x > w.x && b.x < w.x + w.w && b.y > w.y && b.y < w.y + w.h) {
                spawnParticles(b.x, b.y, Color.rgb(200, 180, 100), 4)
                it.remove()
                removed = true
                break
            }
        }
        if (removed) continue

        for (e in enemies) {
            if (b.x > e.x - e.size / 2 && b.x < e.x + e.size / 2 &&
                b.y > e.y - e.size / 2 && b.y < e.y + e.size / 2) {
                e.hp -= 1
                spawnParticles(b.x, b.y, Color.rgb(255, 80, 80), 6)
                it.remove()
                break
            }
        }
    }

    val it2 = enemyBullets.iterator()
    while (it2.hasNext()) {
        val b = it2.next()
        b.update()

        if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
            it2.remove()
            continue
        }

        for (w in walls) {
            if (b.x > w.x && b.x < w.x + w.w && b.y > w.y && b.y < w.y + w.h) {
                it2.remove()
                break
            }
        }
    }
}

private fun updateEnemies() {
    val dead = enemies.filter { it.hp <= 0 }
    for (e in dead) {
        spawnParticles(e.x, e.y, Color.rgb(255, 100, 100), 12)
        dropLoot(e.x, e.y)
        addXp(e.xpReward)
    }
    enemies.removeAll { it.hp <= 0 }

    for (e in enemies) {
        e.update(playerX, playerY, walls, enemies, bullets)

        val dx = playerX - e.x
        val dy = playerY - e.y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist < e.preferredDistance + 80f && e.canShoot()) {
            val len = if (dist < 1f) 1f else dist
            val bullet = Bullet(e.x, e.y, dx / len, dy / len, speed = 10f)
            bullet.isEnemy = true
            enemyBullets.add(bullet)
        }
    }

    val it = enemyBullets.iterator()
    while (it.hasNext()) {
        val b = it.next()
        if (b.x > playerX - playerSize / 2 && b.x < playerX + playerSize / 2 &&
            b.y > playerY - playerSize / 2 && b.y < playerY + playerSize / 2) {
            if (System.currentTimeMillis() - lastPlayerHit > 500) {
                lastPlayerHit = System.currentTimeMillis()
                playerHp -= 1
                spawnParticles(playerX, playerY, Color.rgb(255, 60, 60), 8)
                if (playerHp <= 0) {
                    playerHp = 0
                    gameState = GameState.GAME_OVER
                }
            }
            it.remove()
        }
    }
}

private fun restartGame() {
    playerHp = playerMaxHp
    playerMana = playerMaxMana
    playerXp = 0
    playerLevel = 1
    xpToNextLevel = 30
    coins = 0
    playerSpeed = 8f
    playerMaxHp = 5

    bullets.clear()
    enemyBullets.clear()
    enemies.clear()
    pickups.clear()
    particles.clear()

    generateLevel()
    buildRoomWalls()
    loadRoom(0)

    gameState = GameState.PLAYING
}

private fun drawFrame() {
    var canvas: Canvas? = null
    try {
        canvas = holder.lockCanvas()
        if (canvas != null) drawGame(canvas)
    } finally {
        if (canvas != null) holder.unlockCanvasAndPost(canvas)
    }
}

private fun drawGame(canvas: Canvas) {
    canvas.drawColor(Color.rgb(30, 30, 40))

    // Сетка
    paint.color = Color.rgb(45, 45, 60)
    paint.strokeWidth = 2f
    var x = 0f
    while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), paint); x += 100f }
    var y = 0f
    while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, paint); y += 100f }

    // Стены
    for (w in walls) w.draw(canvas, paint)

    // Двери
    for (d in allDoors) d.draw(canvas, paint)

    // Лут
    for (p in pickups) p.draw(canvas, paint)

    // Враги
    for (e in enemies) e.draw(canvas, paint)

    // Пули
    for (b in bullets) b.draw(canvas, paint)
    for (b in enemyBullets) b.draw(canvas, paint)

    // Частицы
    for (p in particles) p.draw(canvas, paint)

    // Игрок
    paint.color = Color.rgb(80, 200, 120)
    canvas.drawRect(
        playerX - playerSize / 2, playerY - playerSize / 2,
        playerX + playerSize / 2, playerY + playerSize / 2, paint
    )
    paint.color = Color.WHITE
    canvas.drawCircle(playerX, playerY, 5f, paint)

    // HUD
    drawHud(canvas)

    // Джойстики
    moveJoystick?.draw(canvas)
    shootJoystick?.draw(canvas)

    // Экраны
    if (gameState == GameState.GAME_OVER) {
        drawGameOver(canvas)
    } else if (gameState == GameState.VICTORY) {
        drawVictory(canvas)
    } else if (roomCleared && currentRoomIndex < rooms.size - 1) {
        drawRoomClearedHint(canvas)
    }
}

private fun drawHud(canvas: Canvas) {
    val margin = 30f
    val heartSize = 40f
    val heartGap = 12f

    for (i in 0 until playerMaxHp) {
        val hx = margin + i * (heartSize + heartGap)
        val hy = margin
        if (i < playerHp) paint.color = Color.rgb(230, 50, 80)
        else paint.color = Color.rgb(70, 40, 45)
        drawHeart(canvas, hx, hy, heartSize)
    }

    val barX = margin
    val barY = margin + heartSize + 20f
    val barW = 300f
    val barH = 22f

    paint.color = Color.rgb(20, 20, 50)
    val bgRect = RectF(barX, barY, barX + barW, barY + barH)
    canvas.drawRoundRect(bgRect, 8f, 8f, paint)

    val manaPercent = playerMana.toFloat() / playerMaxMana.toFloat()
    paint.color = Color.rgb(70, 130, 240)
    val fillRect = RectF(barX, barY, barX + barW * manaPercent, barY + barH)
    canvas.drawRoundRect(fillRect, 8f, 8f, paint)

    paint.color = Color.rgb(120, 170, 255)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.drawRoundRect(bgRect, 8f, 8f, paint)
    paint.style = Paint.Style.FILL

    paint.color = Color.WHITE
    paint.textSize = 26f
    canvas.drawText("$playerMana / $playerMaxMana", barX + barW + 20f, barY + barH - 4f, paint)

    val xpY = barY + barH + 10f
    paint.color = Color.rgb(40, 40, 20)
    val xpBg = RectF(barX, xpY, barX + barW, xpY + 14f)
    canvas.drawRoundRect(xpBg, 6f, 6f, paint)

    val xpPercent = playerXp.toFloat() / xpToNextLevel.toFloat()
    paint.color = Color.rgb(220, 200, 60)
    val xpFill = RectF(barX, xpY, barX + barW * xpPercent, xpY + 14f)
    canvas.drawRoundRect(xpFill, 6f, 6f, paint)

    paint.color = Color.WHITE
    paint.textSize = 20f
    canvas.drawText("LVL $playerLevel", barX + barW + 20f, xpY + 12f, paint)

    // Справа: комната, монеты, врагов
    paint.textSize = 32f
    paint.color = Color.rgb(255, 220, 80)
    canvas.drawText("💰 $coins", width - 220f, margin + 30f, paint)
    paint.color = Color.WHITE
    canvas.drawText("Комната ${currentRoomIndex + 1}/${rooms.size}", width - 220f, margin + 70f, paint)

    paint.textSize = 22f
    paint.color = Color.rgb(200, 200, 200)
    canvas.drawText("Врагов: ${enemies.size}", width - 220f, margin + 105f, paint)
}

private fun drawRoomClearedHint(canvas: Canvas) {
    paint.color = Color.rgb(120, 220, 150)
    paint.textSize = 40f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Комната зачищена! →", width / 2f, 100f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawGameOver(canvas: Canvas) {
    paint.color = Color.argb(180, 0, 0, 0)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    paint.color = Color.rgb(230, 60, 80)
    paint.textSize = 80f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("GAME OVER", width / 2f, height / 2f - 40f, paint)

    paint.color = Color.WHITE
    paint.textSize = 36f
    canvas.drawText("Монеты: $coins", width / 2f, height / 2f + 30f, paint)
    canvas.drawText("Комната: ${currentRoomIndex + 1}/${rooms.size}", width / 2f, height / 2f + 80f, paint)
    canvas.drawText("Уровень: $playerLevel", width / 2f, height / 2f + 130f, paint)

    paint.textSize = 30f
    paint.color = Color.rgb(120, 220, 150)
    canvas.drawText("Нажми на экран для рестарта", width / 2f, height / 2f + 210f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawVictory(canvas: Canvas) {
    paint.color = Color.argb(180, 0, 0, 20)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    paint.color = Color.rgb(255, 220, 80)
    paint.textSize = 90f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("ПОБЕДА!", width / 2f, height / 2f - 60f, paint)

    paint.color = Color.WHITE
    paint.textSize = 36f
    canvas.drawText("Все комнаты пройдены!", width / 2f, height / 2f + 10f, paint)
    canvas.drawText("Монеты: $coins", width / 2f, height / 2f + 60f, paint)
    canvas.drawText("Уровень: $playerLevel", width / 2f, height / 2f + 110f, paint)

    paint.textSize = 30f
    paint.color = Color.rgb(120, 220, 150)
    canvas.drawText("Нажми для новой игры", width / 2f, height / 2f + 200f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float) {
    val r = size / 4f
    canvas.drawCircle(x + r, y + r, r, paint)
    canvas.drawCircle(x + 3 * r, y + r, r, paint)

    val path = android.graphics.Path()
    path.moveTo(x, y + r)
    path.lineTo(x + size, y + r)
    path.lineTo(x + size / 2, y + size)
    path.close()
    canvas.drawPath(path, paint)
}
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Рестарт после смерти или победы — любой тап
        if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                restartGame()
            }
            return true
        }

        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                val mj = moveJoystick
                if (mj != null && movePointerId == -1 && mj.isInside(x, y)) {
                    movePointerId = pointerId
                    mj.start(x, y)
                    return true
                }

                val sj = shootJoystick
                if (sj != null && shootPointerId == -1 && sj.isInside(x, y)) {
                    shootPointerId = pointerId
                    sj.start(x, y)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (id == movePointerId) {
                        moveJoystick?.move(x, y)
                    } else if (id == shootPointerId) {
                        shootJoystick?.move(x, y)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (pointerId == movePointerId) {
                    moveJoystick?.stop()
                    movePointerId = -1
                } else if (pointerId == shootPointerId) {
                    shootJoystick?.stop()
                    shootPointerId = -1
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                moveJoystick?.stop()
                shootJoystick?.stop()
                movePointerId = -1
                shootPointerId = -1
            }
        }
        return true
    }
}
