package com.ivangames.pixeldungeon

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class WeaponType {
    KNIFE,      // ближний бой, всегда есть, нельзя снять
    PISTOL,     // дальний, стартовое
    SHOTGUN,    // дальний, 3 пули веером
    RIFLE,      // дальний, быстрее стреляет
    SWORD       // ближний, больше урона чем нож
}

class Weapon(
    val type: WeaponType,
    var cooldown: Long,
    var damage: Int,
    var isMelee: Boolean,
    var bulletSpeed: Float = 18f,
    var bulletCount: Int = 1,
    var spreadAngle: Float = 0f
) {
    var lastShot = 0L

    fun canShoot(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastShot > cooldown) {
            lastShot = now
            return true
        }
        return false
    }

    fun resetCooldown() {
        lastShot = System.currentTimeMillis()
    }

    fun getName(): String {
        return when (type) {
            WeaponType.KNIFE -> "Нож"
            WeaponType.PISTOL -> "Пистолет"
            WeaponType.SHOTGUN -> "Дробовик"
            WeaponType.RIFLE -> "Автомат"
            WeaponType.SWORD -> "Меч"
        }
    }

    fun getColor(): Int {
        return when (type) {
            WeaponType.KNIFE -> Color.rgb(200, 200, 220)
            WeaponType.PISTOL -> Color.rgb(255, 220, 80)
            WeaponType.SHOTGUN -> Color.rgb(255, 140, 60)
            WeaponType.RIFLE -> Color.rgb(120, 200, 255)
            WeaponType.SWORD -> Color.rgb(220, 180, 100)
        }
    }

    companion object {
        fun knife() = Weapon(
            WeaponType.KNIFE, cooldown = 400L, damage = 1,
            isMelee = true
        )
        fun pistol() = Weapon(
            WeaponType.PISTOL, cooldown = 220L, damage = 1,
            isMelee = false, bulletSpeed = 18f
        )
        fun shotgun() = Weapon(
            WeaponType.SHOTGUN, cooldown = 800L, damage = 1,
            isMelee = false, bulletSpeed = 14f,
            bulletCount = 3, spreadAngle = 0.3f
        )
        fun rifle() = Weapon(
            WeaponType.RIFLE, cooldown = 100L, damage = 1,
            isMelee = false, bulletSpeed = 20f
        )
        fun sword() = Weapon(
            WeaponType.SWORD, cooldown = 500L, damage = 2,
            isMelee = true
        )
    }
}
