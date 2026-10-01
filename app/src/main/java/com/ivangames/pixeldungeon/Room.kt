package com.ivangames.pixeldungeon

class Room(
    val index: Int,
    val enemyCount: Int,
    val enemyType: Int // 0 — обычный, 1 — быстрый, 2 — танк, 3 — стрелок, 4 — босс
) {
    var cleared = false
    val doors = mutableListOf<Door>()
}
