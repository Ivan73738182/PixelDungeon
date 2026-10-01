package com.ivangames.pixeldungeon

class Room(
    val index: Int,
    val enemyCount: Int,
    val enemyType: Int, // 0-норм, 1-быстрый, 2-танк, 3-стрелок, 4-босс, 5-база
    val isBase: Boolean = false
) {
    var cleared = false
    val doors = mutableListOf<Door>()
}
