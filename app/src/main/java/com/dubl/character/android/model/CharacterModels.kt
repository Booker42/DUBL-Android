package com.dubl.character.android.model

enum class AttributeId(val title: String, val shortTitle: String) {
    STRENGTH("Сила", "СИЛ"),
    DEXTERITY("Ловкость", "ЛОВ"),
    CONSTITUTION("Телосложение", "ТЕЛ"),
    SPEED("Скорость", "СКР"),
    INTELLIGENCE("Интеллект", "ИНТ"),
    PERCEPTION("Восприятие", "ВОС"),
    WILL("Воля", "ВОЛ"),
    CHARISMA("Харизма", "ХАР"),
}

data class AttributeValue(
    val base: Int = 0,
    val bonus: Int = 0,
) {
    val total: Int get() = base + bonus
}

data class DublCharacter(
    val id: String,
    val name: String = "Новый персонаж",
    val concept: String = "",
    val experience: Int = 0,
    val size: Int = 5,
    val legs: Int = 2,
    val attributes: Map<AttributeId, AttributeValue> = defaultAttributes(),
    val hpCurrent: Int = 0,
    val enduranceCurrent: Int = 3,
    val manaEnabled: Boolean = false,
    val manaCurrent: Int = 0,
    val manaMaximum: Int = 0,
    val skills: Map<String, CharacterSkill> = emptyMap(),
    val hiddenSkillIds: Set<String> = emptySet(),
    val development: Map<String, OwnedDevelopment> = emptyMap(),
) {
    fun attributeRaw(id: AttributeId): Int = attributes[id]?.total ?: 0

    fun attribute(id: AttributeId): Int = when (id) {
        AttributeId.STRENGTH -> attributeRaw(id) + (size - 5)
        AttributeId.SPEED -> attributeRaw(id) + (5 - size)
        else -> attributeRaw(id)
    }

    val strength: Int get() = attribute(AttributeId.STRENGTH)
    val dexterity: Int get() = attribute(AttributeId.DEXTERITY)
    val constitution: Int get() = attribute(AttributeId.CONSTITUTION)
    val speed: Int get() = attribute(AttributeId.SPEED)
    val perception: Int get() = attribute(AttributeId.PERCEPTION)
    val will: Int get() = attribute(AttributeId.WILL)

    val defense: Int get() = 10 - size + speed + dexterity
    val healthMaximum: Int get() = (constitution * size + strength).coerceAtLeast(0)
    val reflexes: Int get() = speed + dexterity
    val initiative: Int get() = speed + perception
    val fortitude: Int get() = constitution + will
    val abilityPoints: Int get() = experience.coerceAtLeast(0) / 1000

    val runBase: Double
        get() = if (legs >= 3) {
            when (size.coerceIn(1, 10)) {
                1 -> 2.0
                2 -> 4.0
                3 -> 4.0
                4 -> 8.0
                5 -> 12.0
                6 -> 20.0
                7 -> 32.0
                8 -> 40.0
                9 -> 56.0
                else -> 80.0
            }
        } else {
            when (size.coerceIn(1, 10)) {
                1 -> 1.0
                2 -> 2.0
                3 -> 4.0
                4 -> 6.0
                5 -> 8.0
                6 -> 12.0
                7 -> 18.0
                8 -> 24.0
                9 -> 32.0
                else -> 60.0
            }
        }

    val runFull: Double
        get() {
            val multiplier = if (legs >= 3) {
                when (size.coerceIn(1, 10)) {
                    1 -> 0.5
                    2 -> 1.0
                    3 -> 1.5
                    4, 5, 6 -> 2.0
                    7 -> 3.0
                    8 -> 4.0
                    9 -> 5.0
                    else -> 6.0
                }
            } else {
                when (size.coerceIn(1, 10)) {
                    1 -> 0.125
                    2 -> 0.25
                    3 -> 0.5
                    4, 5 -> 1.0
                    6, 7 -> 1.5
                    8 -> 2.0
                    9 -> 3.0
                    else -> 4.0
                }
            }
            return (runBase + speed * multiplier).coerceAtLeast(0.0)
        }

    fun normalized(): DublCharacter {
        val normalizedSkills = skills.mapValues { (_, skill) ->
            skill.copy(
                rank = skill.rank.coerceIn(0, 10),
                attributes = skill.attributes.distinct(),
            )
        }
        val clamped = copy(
            experience = experience.coerceAtLeast(0),
            size = size.coerceIn(1, 10),
            legs = legs.coerceAtLeast(2),
            enduranceCurrent = enduranceCurrent.coerceIn(0, 3),
            manaMaximum = manaMaximum.coerceAtLeast(0),
            skills = normalizedSkills,
            hiddenSkillIds = hiddenSkillIds.filterTo(linkedSetOf()) { id ->
                SkillCatalog.builtIns.any { it.id == id } || normalizedSkills.containsKey(id)
            },
            development = development.mapNotNull { (id, owned) ->
                val rank = owned.rank.coerceAtLeast(0)
                if (rank == 0 || id.isBlank()) null else id to owned.copy(
                    rank = rank,
                    optionIndex = owned.optionIndex.coerceAtLeast(0),
                )
            }.toMap(linkedMapOf()),
        )
        return clamped.copy(
            hpCurrent = clamped.hpCurrent.coerceIn(0, clamped.healthMaximum),
            manaCurrent = clamped.manaCurrent.coerceIn(0, clamped.manaMaximum),
        )
    }
}

data class AppSnapshot(
    val characters: List<DublCharacter>,
    val activeCharacterId: String,
) {
    val activeCharacter: DublCharacter
        get() = characters.firstOrNull { it.id == activeCharacterId }
            ?: characters.first()
}

fun defaultAttributes(): Map<AttributeId, AttributeValue> =
    AttributeId.entries.associateWith { AttributeValue() }
