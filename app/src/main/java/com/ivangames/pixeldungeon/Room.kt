package com.ivangames.pixeldungeon

enum class RoomKind {
    BASE,       // база с мишенями
    FIGHT,      // обычная боевая
    MIXED,      // смешанные враги
    TREASURE,   // сокровищница (много монет, ловушки)
    SHOP,       // магазин
    BOSS        // босс
}

class Room(
    val index: Int,
    val kind: RoomKind,
    val enemyTypes: List<Int>,   // список типов врагов (0-4)
    val enemyCountPerType: Int,  // сколько каждого
    val size: Int = 1            // 0-маленькая, 1-средняя, 2-большая
) {
    var cleared = false
    val doors = mutableListOf<Door>()

    // Внутренние препятствия (колонны, блоки) для этой комнаты
    val innerWalls = mutableListOf<Wall>()
    // Ловушки
    val traps = mutableListOf<Trap>()
    // Сундук
    var chest: Chest? = null
    // Флаг магазина
    var shopVisited = false
}
