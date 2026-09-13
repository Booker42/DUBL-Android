package com.dubl.character.android.model

import kotlin.math.max

data class MagicSchool(
    val name: String = "Школа",
    val rank: Int = 0,
    val note: String = "",
)

data class KnownSpell(
    val uid: String,
    val catalogId: String? = null,
    val name: String = "Заклинание",
    val school: String = "",
    val cost: Int = 0,
    val manaText: String = "",
    val time: String = "",
    val range: String = "",
    val area: String = "",
    val action: String = "",
    val duration: String = "",
    val description: String = "",
    val enhancement: String = "",
    val learned: Boolean = true,
    val xpOverride: Int? = null,
    val incomplete: Boolean = false,
    val conflictNote: String = "",
    val custom: Boolean = false,
)

data class CharacterMagic(
    val manaRank: Int = 0,
    val power: Int = 0,
    val schools: List<MagicSchool> = emptyList(),
    val spells: List<KnownSpell> = emptyList(),
)

data class GearItem(
    val uid: String,
    val catalogId: String? = null,
    val name: String = "Предмет",
    val quantity: Int = 1,
    val load: Double = 0.0,
    val carried: Boolean = true,
    val description: String = "",
    val category: String = "Снаряжение",
    val section: String = "Предметы",
    val fields: Map<String, String> = emptyMap(),
    val custom: Boolean = false,
)

data class CharacterGear(
    val loadAutomatic: Boolean = true,
    val loadManual: Double = 0.0,
    val items: List<GearItem> = emptyList(),
)

data class SpellCatalogEntry(
    val id: String,
    val name: String,
    val school: String,
    val cost: Int,
    val manaText: String,
    val time: String,
    val range: String,
    val area: String,
    val action: String,
    val duration: String,
    val description: String,
    val enhancement: String,
    val incomplete: Boolean,
    val conflictNote: String,
)

data class GearCatalogEntry(
    val id: String,
    val name: String,
    val category: String,
    val section: String,
    val fields: Map<String, String>,
    val description: String,
)

data class MagicEquipmentCatalog(
    val version: String,
    val spells: List<SpellCatalogEntry>,
    val gear: List<GearCatalogEntry>,
)

data class BurdenState(
    val title: String,
    val penalty: Int,
)

object MagicEquipmentRules {
    const val BASE_MANA_ENTRY_ID = "feat_bf7b04eb68961725"
    const val INCREASED_MANA_ENTRY_ID = "feat_fd7bee8198e57574"
    const val MEDITATION_ENTRY_ID = "feat_f6f37c05cf7c5257"

    private val manaTable = arrayOf(
        intArrayOf(1, 2, 3, 4, 5),
        intArrayOf(2, 4, 6, 8, 10),
        intArrayOf(3, 6, 9, 12, 15),
        intArrayOf(4, 8, 12, 16, 20),
        intArrayOf(5, 10, 15, 20, 25),
        intArrayOf(6, 13, 19, 33, 45),
        intArrayOf(7, 16, 25, 46, 65),
        intArrayOf(8, 19, 31, 59, 85),
        intArrayOf(9, 22, 37, 72, 105),
        intArrayOf(10, 25, 43, 85, 125),
        intArrayOf(11, 30, 53, 107, 155),
        intArrayOf(12, 35, 63, 129, 185),
        intArrayOf(13, 40, 73, 151, 215),
        intArrayOf(14, 45, 83, 173, 245),
        intArrayOf(15, 50, 93, 195, 275),
        intArrayOf(16, 56, 106, 226, 320),
        intArrayOf(17, 62, 119, 257, 365),
        intArrayOf(18, 68, 132, 288, 410),
        intArrayOf(19, 74, 145, 319, 455),
        intArrayOf(20, 80, 160, 350, 500),
    )

    fun manaMaximum(character: DublCharacter): Int {
        val rank = character.magic.manaRank.coerceIn(0, 5)
        val power = character.magic.power.coerceAtLeast(0)
        if (rank <= 0 || power <= 0) return 0
        val basePower = power.coerceAtMost(20)
        var value = manaTable[basePower - 1][rank - 1]
        if (power > 20) {
            value += (power - 20) * intArrayOf(2, 7, 17, 40, 55)[rank - 1]
        }
        val increasedManaRank = character.development[INCREASED_MANA_ENTRY_ID]?.rank ?: 0
        return max(0, value + increasedManaRank * rank)
    }

    fun manaRecoveryPerRound(character: DublCharacter): Int =
        1 + (character.development[MEDITATION_ENTRY_ID]?.rank ?: 0)

    fun learnXpCost(manaCost: Int): Int? = when (manaCost) {
        in 0..3 -> 10
        in 4..8 -> 20
        in 9..12 -> 30
        in 13..16 -> 40
        in 17..20 -> 50
        else -> null
    }

    fun learnedSpellXp(character: DublCharacter): Int = character.magic.spells
        .filter { it.learned }
        .sumOf { it.xpOverride ?: learnXpCost(it.cost) ?: 0 }

    fun manaRankXp(character: DublCharacter): Int = character.magic.manaRank.coerceIn(0, 5) * 100


    fun catalogGearLoad(entry: GearCatalogEntry): Double {
        val raw = entry.fields["Вес"]
            ?: entry.fields["Вес, кг"]
            ?: return 0.0
        val match = Regex("""\d+(?:[.,]\d+)?""").find(raw) ?: return 0.0
        return match.value.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    fun equipmentLoad(character: DublCharacter): Double {
        if (!character.gear.loadAutomatic) return character.gear.loadManual.coerceAtLeast(0.0)
        return character.gear.items
            .asSequence()
            .filter { it.carried }
            .sumOf { it.load.coerceAtLeast(0.0) * it.quantity.coerceAtLeast(0) }
    }

    fun equipmentCapacity(character: DublCharacter): Int =
        (character.strength + character.constitution).coerceAtLeast(0)

    fun burden(character: DublCharacter): BurdenState {
        val load = equipmentLoad(character)
        val capacity = equipmentCapacity(character).toDouble()
        return when {
            load <= 0.0 || (capacity > 0.0 && load < capacity) -> BurdenState("Нет нагрузки", 0)
            capacity <= 0.0 -> BurdenState("Нагрузка при нулевой вместимости", -4)
            load <= capacity * 1.5 -> BurdenState("Лёгкая нагрузка", -1)
            load <= capacity * 2.0 -> BurdenState("Средняя нагрузка", -2)
            load <= capacity * 3.0 -> BurdenState("Тяжёлая нагрузка", -4)
            else -> BurdenState("Свыше таблицы нагрузки", -4)
        }
    }
}
