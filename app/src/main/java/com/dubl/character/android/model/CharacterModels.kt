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
    val creationExperience: Int = 0,
    val creationComplete: Boolean = false,
    val xpAdjustment: Int = 0,
    val abilityPointsOverride: Int? = null,
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
    val magic: CharacterMagic = CharacterMagic(),
    val gear: CharacterGear = CharacterGear(),
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

    val equipmentLoadPenalty: Int get() = MagicEquipmentRules.burden(this).penalty
    val defense: Int get() = 10 - size + speed + dexterity + equipmentLoadPenalty
    val healthMaximum: Int get() = (constitution * size + strength).coerceAtLeast(0)
    val reflexes: Int get() = speed + dexterity + equipmentLoadPenalty
    val initiative: Int get() = speed + perception
    val fortitude: Int get() = constitution + will
    val effectiveCreationExperience: Int
        get() = when {
            creationExperience > 0 -> creationExperience.coerceAtMost(experience.coerceAtLeast(0))
            !creationComplete -> experience.coerceAtLeast(0)
            else -> 0
        }
    val recommendedAbilityPoints: Int get() = effectiveCreationExperience / 1000
    val abilityPoints: Int get() = abilityPointsOverride ?: recommendedAbilityPoints
    val effectiveManaMaximum: Int
        get() = if (magic.manaRank > 0) MagicEquipmentRules.manaMaximum(this) else manaMaximum.coerceAtLeast(0)

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
            return (runBase + speed * multiplier + equipmentLoadPenalty).coerceAtLeast(0.0)
        }

    fun normalized(): DublCharacter {
        val normalizedSkills = skills.mapValues { (_, skill) ->
            skill.copy(
                rank = skill.rank.coerceIn(0, 10),
                attributes = skill.attributes.distinct(),
            )
        }
        val normalizedAttributes = attributes.mapValues { (_, value) ->
            value.copy(base = value.base.coerceIn(-5, 10))
        }
        val legacyManaRank = development[MagicEquipmentRules.BASE_MANA_ENTRY_ID]?.rank?.coerceIn(0, 5) ?: 0
        val normalizedManaRank = maxOf(magic.manaRank.coerceIn(0, 5), legacyManaRank)
        val normalizedMagic = magic.copy(
            manaRank = normalizedManaRank,
            power = if (normalizedManaRank > 0) magic.power.coerceAtLeast(1) else magic.power.coerceAtLeast(0),
            schools = magic.schools.map { school ->
                school.copy(name = school.name.trim().ifBlank { "Школа" }, rank = school.rank.coerceAtLeast(0), note = school.note.trim())
            },
            spells = magic.spells.map { spell ->
                spell.copy(
                    name = spell.name.trim().ifBlank { "Заклинание" },
                    school = spell.school.trim(),
                    cost = spell.cost.coerceAtLeast(0),
                    learned = spell.learned,
                    xpOverride = spell.xpOverride?.coerceAtLeast(0),
                )
            }.distinctBy { it.uid },
        )
        val normalizedGear = gear.copy(
            loadManual = gear.loadManual.coerceAtLeast(0.0),
            items = gear.items.map { item ->
                item.copy(
                    name = item.name.trim().ifBlank { "Предмет" },
                    quantity = item.quantity.coerceAtLeast(1),
                    load = item.load.coerceAtLeast(0.0),
                )
            }.distinctBy { it.uid },
        )
        val normalizedExperience = experience.coerceAtLeast(0)
        val normalizedCreationExperience = creationExperience.coerceAtLeast(0).coerceAtMost(normalizedExperience)
        val clamped = copy(
            experience = normalizedExperience,
            creationExperience = normalizedCreationExperience,
            xpAdjustment = xpAdjustment.coerceIn(-1_000_000, 1_000_000),
            abilityPointsOverride = abilityPointsOverride?.coerceAtLeast(0),
            size = size.coerceIn(1, 10),
            attributes = normalizedAttributes,
            legs = legs.coerceAtLeast(2),
            enduranceCurrent = enduranceCurrent.coerceIn(0, 3),
            manaMaximum = manaMaximum.coerceAtLeast(0),
            skills = normalizedSkills,
            hiddenSkillIds = hiddenSkillIds.filterTo(linkedSetOf()) { id ->
                SkillCatalog.builtIns.any { it.id == id } || normalizedSkills.containsKey(id)
            },
            development = development.mapNotNull { (id, owned) ->
                val rank = owned.rank.coerceAtLeast(0)
                if (rank == 0 || id.isBlank() || id == MagicEquipmentRules.BASE_MANA_ENTRY_ID) null else id to owned.copy(
                    rank = rank,
                    optionIndex = owned.optionIndex.coerceAtLeast(0),
                )
            }.toMap(linkedMapOf()),
            magic = normalizedMagic,
            gear = normalizedGear,
        )
        val maxMana = clamped.effectiveManaMaximum
        return clamped.copy(
            hpCurrent = clamped.hpCurrent.coerceIn(0, clamped.healthMaximum),
            manaCurrent = clamped.manaCurrent.coerceIn(0, maxMana),
            manaEnabled = clamped.manaEnabled || clamped.magic.manaRank > 0,
            manaMaximum = if (clamped.magic.manaRank > 0) maxMana else clamped.manaMaximum.coerceAtLeast(0),
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
