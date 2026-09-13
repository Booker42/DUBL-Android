package com.dubl.character.android.model

enum class DevelopmentCostType {
    XP,
    ABILITY,
}

data class AbilityOption(
    val source: String,
    val value: Int,
)

data class DevelopmentEntry(
    val id: String,
    val name: String,
    val section: String,
    val category: String,
    val cost: Int,
    val costType: DevelopmentCostType,
    val maxRank: Int,
    val requirements: String,
    val benefit: String,
    val notes: String,
    val tags: List<String>,
    val accessId: String?,
    val abilityOptions: List<AbilityOption>,
    val incomplete: Boolean,
    val repeatable: Boolean,
    val perfectRoot: Boolean,
    val mechanicsConflict: String,
    val conflictNote: String,
) {
    val isAbility: Boolean get() = costType == DevelopmentCostType.ABILITY

    /**
     * Special development is intentionally broader than accessId != null.
     * Some rulebook branches (perfect abilities / animagia / entropy) are XP-only
     * and therefore have no separate ability-point access record, but still belong
     * to the special-branch catalogue rather than the ordinary feat list.
     */
    val isSpecialDevelopment: Boolean
        get() = isAbility || accessId != null || developmentNormalize(section) == "ветки способностей"

    val isRegularDevelopment: Boolean
        get() = !isSpecialDevelopment && costType == DevelopmentCostType.XP
}

data class OwnedDevelopment(
    val rank: Int = 0,
    val optionIndex: Int = 0,
)

data class DevelopmentProgress(
    val owned: Map<String, OwnedDevelopment> = emptyMap(),
) {
    fun rank(entryId: String): Int = owned[entryId]?.rank ?: 0
    fun optionIndex(entryId: String): Int = owned[entryId]?.optionIndex ?: 0

    fun withRank(entry: DevelopmentEntry, rank: Int, optionIndex: Int = optionIndex(entry.id)): DevelopmentProgress {
        val normalizedRank = rank.coerceIn(0, entry.maxRank.coerceAtLeast(1))
        val next = owned.toMutableMap()
        if (normalizedRank == 0) {
            next.remove(entry.id)
        } else {
            next[entry.id] = OwnedDevelopment(
                rank = normalizedRank,
                optionIndex = optionIndex.coerceIn(0, (entry.abilityOptions.size - 1).coerceAtLeast(0)),
            )
        }
        return copy(owned = next)
    }
}

enum class RequirementStatus {
    OK,
    FAIL,
    MANUAL,
}

data class RequirementCheck(
    val status: RequirementStatus,
    val text: String,
    val targetEntryId: String? = null,
)

data class DevelopmentAvailability(
    val checks: List<RequirementCheck>,
    val currentRank: Int,
    val maxRank: Int,
    val abilityCost: Int,
    val canIncrease: Boolean,
    val canForceIncrease: Boolean,
    val reason: String,
)

class DevelopmentCatalog(
    val version: String,
    val entries: List<DevelopmentEntry>,
) {
    private val byIdMap = entries.associateBy { it.id }
    private val byNameMap = entries.groupBy { developmentAlias(it.name) }

    fun byId(id: String?): DevelopmentEntry? = id?.let(byIdMap::get)

    fun matchingName(name: String): List<DevelopmentEntry> {
        val normalized = developmentAlias(name)
        val exact = byNameMap[normalized].orEmpty()
        if (exact.isNotEmpty()) return exact
        return entries.filter { entry ->
            entry.name.split(" / ").any { developmentAlias(it) == normalized }
        }
    }

    fun childrenOf(entryId: String): List<DevelopmentEntry> = entries
        .filter { it.accessId == entryId }
        .sortedBy { developmentNormalize(it.name) }
}

private val DEVELOPMENT_ALIASES = mapOf(
    "ассасин" to "ассассин",
    "боевой крик" to "боевые крики",
    "мастер ловушек" to "капканщик / мастер ловушек",
    "ул. инициатива" to "улучшенная инициатива",
    "инженерное дело" to "инженерное дело (ремонт)",
    "тело" to "телосложение",
    "рукопашный" to "рукопашный бой",
    "рукопашное" to "рукопашный бой",
    "холодное" to "холодное оружие",
)

fun developmentNormalize(value: String): String = value
    .lowercase()
    .replace('ё', 'е')
    .replace(Regex("\\s+"), " ")
    .trim(' ', '.', ',', ':', ';')

fun developmentAlias(value: String): String {
    val normalized = developmentNormalize(value)
    return DEVELOPMENT_ALIASES[normalized] ?: normalized
}

class DevelopmentRules(
    private val character: DublCharacter,
    private val catalog: DevelopmentCatalog,
    private val progress: DevelopmentProgress,
) {
    private val requirementsMemo = mutableMapOf<String, List<RequirementCheck>>()
    private val allSkills by lazy { character.resolvedSkills(includeHidden = true) }

    fun requirements(entry: DevelopmentEntry): List<RequirementCheck> =
        requirementsInternal(entry, emptySet())

    fun abilityPointsSpent(): Int = progress.owned.entries.sumOf { (id, owned) ->
        val entry = catalog.byId(id) ?: return@sumOf 0
        if (!entry.isAbility) return@sumOf 0
        abilityCost(entry, owned.optionIndex) * owned.rank
    }

    fun abilityPointsBudget(): Int = character.abilityPoints

    fun abilityPointsAvailable(): Int = abilityPointsBudget() - abilityPointsSpent()

    fun xpSpentOnDevelopment(): Int = progress.owned.entries.sumOf { (id, owned) ->
        if (id == MagicEquipmentRules.BASE_MANA_ENTRY_ID) return@sumOf 0
        val entry = catalog.byId(id) ?: return@sumOf 0
        if (entry.isAbility) 0 else entry.cost.coerceAtLeast(0) * owned.rank
    }

    fun abilityCost(entry: DevelopmentEntry, optionIndex: Int): Int {
        if (!entry.isAbility) return 0
        if (entry.abilityOptions.isNotEmpty()) {
            return entry.abilityOptions[optionIndex.coerceIn(0, entry.abilityOptions.lastIndex)].value.coerceAtLeast(0)
        }
        return entry.cost.coerceAtLeast(0)
    }

    fun availability(entry: DevelopmentEntry, optionIndex: Int = progress.optionIndex(entry.id)): DevelopmentAvailability {
        val currentRank = progress.rank(entry.id)
        val checks = requirements(entry)
        val abilityCost = abilityCost(entry, optionIndex)
        val failed = checks.any { it.status == RequirementStatus.FAIL }
        val manual = checks.any { it.status == RequirementStatus.MANUAL }
        val maxed = currentRank >= entry.maxRank.coerceAtLeast(1)
        // The rulebook phrases the OS budget as a recommendation, not a hard limit.
        // Overspending is surfaced by the budget summary but does not invalidate a GM-approved build.
        val canIncrease = !entry.incomplete && !failed && !manual && !maxed
        val canForceIncrease = !entry.incomplete && !maxed && (failed || manual)
        val reason = when {
            entry.incomplete -> "Запись книги не завершена"
            maxed -> "Максимальный ранг"
            failed -> "Не выполнены требования"
            manual -> "Требуется ручная проверка"
            else -> "Можно получить"
        }
        return DevelopmentAvailability(
            checks = checks,
            currentRank = currentRank,
            maxRank = entry.maxRank.coerceAtLeast(1),
            abilityCost = abilityCost,
            canIncrease = canIncrease,
            canForceIncrease = canForceIncrease,
            reason = reason,
        )
    }

    fun featureRank(name: String): Int {
        if (developmentAlias(name) == developmentAlias("Базовый запас маны")) return character.magic.manaRank
        val known = catalog.matchingName(name)
        val preferred = known.filterNot { it.isAbility }.ifEmpty { known }
        return preferred.maxOfOrNull { progress.rank(it.id) } ?: 0
    }

    private fun requirementsInternal(entry: DevelopmentEntry, seen: Set<String>): List<RequirementCheck> {
        if (entry.id !in seen) {
            requirementsMemo[entry.id]?.let { return it }
        }

        val checks = mutableListOf<RequirementCheck>()
        val nextSeen = seen + entry.id

        entry.accessId?.let { accessId ->
            val access = catalog.byId(accessId)
            if (access == null) {
                checks += RequirementCheck(RequirementStatus.MANUAL, "Неизвестная ветка доступа")
            } else {
                val owned = progress.rank(access.id) > 0
                val status = if (!owned) {
                    RequirementStatus.FAIL
                } else if (access.id in seen) {
                    RequirementStatus.MANUAL
                } else {
                    val previous = requirementsInternal(access, nextSeen)
                    when {
                        previous.any { it.status == RequirementStatus.FAIL } -> RequirementStatus.FAIL
                        previous.any { it.status == RequirementStatus.MANUAL } -> RequirementStatus.MANUAL
                        else -> RequirementStatus.OK
                    }
                }
                checks += RequirementCheck(
                    status,
                    "Доступ к ветке: ${access.name}",
                    access.id,
                )
            }
        }

        if (entry.perfectRoot) {
            val other = progress.owned.keys
                .mapNotNull(catalog::byId)
                .filter { it.perfectRoot && it.id != entry.id && progress.rank(it.id) > 0 }
            if (other.isNotEmpty()) {
                checks += RequirementCheck(
                    RequirementStatus.FAIL,
                    "Только одна совершенная способность; уже выбрана: ${other.joinToString { it.name }}",
                    other.first().id,
                )
            }
        }

        val raw = entry.requirements.trim(' ', '.', ',', ';')
        if (raw.isNotBlank() && raw != "-" && raw != "—") {
            val parts = raw.split(Regex("[,;\\n]+(?![^()]*\\))"))
            parts.filter { it.isNotBlank() }.forEach { part ->
                checks += checkAtom(part, entry, nextSeen)
            }
        }

        if (entry.mechanicsConflict.isNotBlank()) {
            checks += RequirementCheck(RequirementStatus.MANUAL, entry.mechanicsConflict)
        }
        if (entry.incomplete) {
            checks += RequirementCheck(
                RequirementStatus.MANUAL,
                "В исходной записи не завершены механика или реквизиты",
            )
        }

        val result = checks.ifEmpty {
            listOf(RequirementCheck(RequirementStatus.OK, "Без требований"))
        }
        if (entry.id !in seen) requirementsMemo[entry.id] = result
        return result
    }

    private fun checkAtom(
        source: String,
        entry: DevelopmentEntry,
        seen: Set<String>,
    ): RequirementCheck {
        val text = source.trim(' ', '.', ';')
        if (text.isBlank() || text == "-" || text == "—") {
            return RequirementCheck(RequirementStatus.OK, "Без требований")
        }

        if (Regex("только\\s+при\\s+создании|при\\s+создании\\s+персонажа", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            val alreadyOwned = progress.rank(entry.id) > 0
            return RequirementCheck(
                if (!character.creationComplete || alreadyOwned) RequirementStatus.OK else RequirementStatus.FAIL,
                if (character.creationComplete && !alreadyOwned) "$text — создание уже завершено" else text,
            )
        }
        if (Regex("на усмотрение|согласован", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return RequirementCheck(RequirementStatus.MANUAL, text)
        }

        if (Regex("\\sили\\s", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            val parts = text.split(Regex("\\s+или\\s+", RegexOption.IGNORE_CASE))
            val trailingRank = Regex("\\s(\\d+)$").find(parts.last())?.groupValues?.getOrNull(1)
            val expanded = if (trailingRank != null) {
                parts.map { part ->
                    if (Regex("\\d+$").containsMatchIn(part.trim())) part else "$part $trailingRank"
                }
            } else {
                parts
            }
            val alternatives = expanded.map { checkAtom(it, entry, seen) }
            val status = when {
                alternatives.any { it.status == RequirementStatus.OK } -> RequirementStatus.OK
                alternatives.any { it.status == RequirementStatus.MANUAL } -> RequirementStatus.MANUAL
                else -> RequirementStatus.FAIL
            }
            return RequirementCheck(
                status = status,
                text = alternatives.joinToString(" или ") { it.text },
                targetEntryId = alternatives.firstOrNull { it.status == RequirementStatus.OK }?.targetEntryId
                    ?: alternatives.firstOrNull()?.targetEntryId,
            )
        }

        if (Regex("^(?:Базовый\\s+)?[Зз]апас маны", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            val needMana = Regex("(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            return valueCheck("Базовый запас маны", character.magic.manaRank, needMana)
        }
        if (developmentNormalize(text).startsWith("заклинание:")) {
            val requested = text.substringAfter(':').trim()
            val learned = character.magic.spells.any { it.learned && developmentAlias(it.name) == developmentAlias(requested) }
            return RequirementCheck(
                if (learned) RequirementStatus.OK else RequirementStatus.FAIL,
                if (learned) "Заклинание: $requested" else "Заклинание: $requested — не изучено",
            )
        }
        val spellCountRequirement = Regex("^Знать\\s*(\\d+)\\s*заклинани", RegexOption.IGNORE_CASE).find(text)
        if (spellCountRequirement != null) {
            val needSpells = spellCountRequirement.groupValues[1].toIntOrNull() ?: 0
            val haveSpells = character.magic.spells.count { it.learned }
            return valueCheck("Изученные заклинания", haveSpells, needSpells)
        }

        val numeric = Regex("^(.+?)\\s*:?\\s*(\\d+)(?:\\s*ранг(?:а|ов)?)?$", RegexOption.IGNORE_CASE)
            .matchEntire(text)
        var featureName = developmentAlias(text)
        var need = 1
        if (numeric != null) {
            val originalName = numeric.groupValues[1].trim()
            featureName = developmentAlias(originalName)
            need = numeric.groupValues[2].toIntOrNull() ?: 1
            numericRequirement(originalName, featureName, need)?.let { return it }
        } else {
            skillRequirement(text, 1)?.let { return it }
        }

        val known = catalog.matchingName(featureName)
        val preferred = known.filterNot { it.isAbility }.ifEmpty { known }
        if (preferred.isNotEmpty()) {
            val maximum = preferred.maxOf { it.maxRank.coerceAtLeast(1) }
            if (need > maximum) {
                return RequirementCheck(
                    RequirementStatus.MANUAL,
                    "$text: требование выше максимального ранга в книге",
                    preferred.first().id,
                )
            }
            val owned = preferred.filter { progress.rank(it.id) >= need }
            if (owned.isEmpty()) {
                return RequirementCheck(
                    RequirementStatus.FAIL,
                    "$text: не изучено",
                    preferred.first().id,
                )
            }
            for (candidate in owned) {
                if (candidate.id in seen) {
                    return RequirementCheck(RequirementStatus.MANUAL, "$text: цикл требований", candidate.id)
                }
                val previous = requirementsInternal(candidate, seen + candidate.id)
                if (previous.all { it.status == RequirementStatus.OK }) {
                    return RequirementCheck(RequirementStatus.OK, text, candidate.id)
                }
            }
            val previousStatuses = owned.flatMap { requirementsInternal(it, seen + it.id) }
            val status = if (previousStatuses.any { it.status == RequirementStatus.FAIL }) {
                RequirementStatus.FAIL
            } else {
                RequirementStatus.MANUAL
            }
            return RequirementCheck(
                status,
                "$text: проверьте требования предшествующего навыка",
                owned.first().id,
            )
        }

        val battleCryMatch = Regex("^Любые (два|три) боевых крика", RegexOption.IGNORE_CASE).find(text)
        if (battleCryMatch != null) {
            val needCries = if (battleCryMatch.groupValues[1].lowercase() == "два") 2 else 3
            val have = progress.owned.keys.count { id ->
                val owned = progress.rank(id) > 0
                val feat = catalog.byId(id)
                owned && feat?.tags?.any { it == "Боевой крик" } == true
            }
            return RequirementCheck(
                if (have >= needCries) RequirementStatus.OK else RequirementStatus.FAIL,
                "Боевые крики: $have / $needCries",
            )
        }

        return RequirementCheck(RequirementStatus.MANUAL, text)
    }

    private fun numericRequirement(
        originalName: String,
        normalizedName: String,
        need: Int,
    ): RequirementCheck? {
        val attribute = AttributeId.entries.firstOrNull {
            developmentAlias(it.title) == normalizedName || developmentAlias(it.shortTitle) == normalizedName
        }
        if (attribute != null) {
            val actual = character.attribute(attribute)
            return valueCheck(originalName, actual, need)
        }

        when (normalizedName) {
            "стойкость" -> return valueCheck(originalName, character.fortitude + featureRank("Стойкий"), need)
            "любая характеристика" -> return valueCheck(originalName, AttributeId.entries.maxOf(character::attribute), need)
            "любые две характеристики" -> {
                val values = AttributeId.entries.map(character::attribute).sortedDescending()
                return valueCheck(originalName, values.getOrElse(1) { 0 }, need)
            }
            "любое умение" -> return valueCheck(originalName, allSkills.maxOfOrNull { it.rank } ?: 0, need)
            "любое умение ближнего боя" -> {
                val actual = maxOf(skillRank("Холодное оружие"), skillRank("Рукопашный бой"))
                return valueCheck(originalName, actual, need)
            }
            "сила магии", "сила заклинаний" -> return valueCheck(originalName, MagicEquipmentRules.highestMagicPower(character), need)
        }

        if (normalizedName.startsWith("любые два умения")) {
            val ranks = allSkills.map { it.rank }.sortedDescending()
            return valueCheck(originalName, ranks.getOrElse(1) { 0 }, need)
        }

        skillRequirement(originalName, need)?.let { return it }

        val family = when {
            Regex("^знани[ея](\\s*\\(любое\\))?$", RegexOption.IGNORE_CASE).matches(originalName) -> "знание"
            Regex("^ремесло(\\s*\\(любое\\))?$", RegexOption.IGNORE_CASE).matches(originalName) -> "ремесло"
            Regex("^исполнение(\\s*\\(любое\\))?$", RegexOption.IGNORE_CASE).matches(originalName) -> "исполнение"
            else -> null
        }
        if (family != null) {
            val actual = allSkills
                .filter { developmentNormalize(it.name).startsWith(family) }
                .maxOfOrNull { it.rank } ?: 0
            return valueCheck(originalName, actual, need)
        }

        return null
    }

    private fun skillRequirement(name: String, need: Int): RequirementCheck? {
        val normalized = developmentAlias(name)
        val exact = allSkills.filter { developmentAlias(it.name) == normalized }
        if (exact.isNotEmpty()) {
            return valueCheck(name, exact.maxOf { it.rank }, need)
        }

        // The rulebook frequently shortens combat-skill names in requirements
        // (e.g. "Рукопашный" / "Холодное"). Accept a unique prefix,
        // but never guess when more than one skill could match.
        val prefixMatches = allSkills.filter { skill ->
            val skillName = developmentAlias(skill.name)
            normalized.length >= 4 && (skillName.startsWith(normalized) || normalized.startsWith(skillName))
        }
        if (prefixMatches.map { developmentAlias(it.name) }.distinct().size == 1 && prefixMatches.isNotEmpty()) {
            return valueCheck(name, prefixMatches.maxOf { it.rank }, need)
        }
        return null
    }

    private fun skillRank(name: String): Int = allSkills
        .filter { developmentAlias(it.name) == developmentAlias(name) }
        .maxOfOrNull { it.rank } ?: 0

    private fun valueCheck(label: String, actual: Int, need: Int): RequirementCheck = RequirementCheck(
        status = if (actual >= need) RequirementStatus.OK else RequirementStatus.FAIL,
        text = "$label: $actual / $need",
    )
}
