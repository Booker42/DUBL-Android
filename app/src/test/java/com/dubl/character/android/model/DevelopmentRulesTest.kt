package com.dubl.character.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentRulesTest {
    private val access = DevelopmentEntry(
        id = "access_assassin",
        name = "Ассасин",
        section = "Особые способности",
        category = "Особые способности",
        cost = 0,
        costType = DevelopmentCostType.ABILITY,
        maxRank = 1,
        requirements = "Скрытность 4",
        benefit = "Открывает ветку",
        notes = "",
        tags = emptyList(),
        accessId = null,
        abilityOptions = listOf(AbilityOption("Мастерство", 1)),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )

    private val child = DevelopmentEntry(
        id = "feat_sneak_attack",
        name = "Подлая атака",
        section = "Ветки способностей",
        category = "Ассасин",
        cost = 40,
        costType = DevelopmentCostType.XP,
        maxRank = 2,
        requirements = "Ассасин, Ловкость 4",
        benefit = "Тест",
        notes = "",
        tags = listOf("Ассасин"),
        accessId = access.id,
        abilityOptions = emptyList(),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )

    private val catalog = DevelopmentCatalog("test", listOf(access, child))

    private fun character(stealthRank: Int = 0, dexterity: Int = 0, xp: Int = 1000): DublCharacter {
        val skills = if (stealthRank > 0) {
            mapOf("stealth" to CharacterSkill(id = "stealth", definitionId = "stealth", rank = stealthRank))
        } else emptyMap()
        return DublCharacter(
            id = "test",
            experience = xp,
            attributes = defaultAttributes() + (
                AttributeId.DEXTERITY to AttributeValue(base = dexterity)
            ),
            skills = skills,
        )
    }

    @Test
    fun abilityUsesExperienceBudgetAndRequirements() {
        val rules = DevelopmentRules(character(stealthRank = 4), catalog, DevelopmentProgress())
        assertEquals(1, rules.abilityPointsBudget())
        assertEquals(1, rules.abilityPointsAvailable())
        assertTrue(rules.availability(access).canIncrease)
    }

    @Test
    fun childRequiresBranchAndAttribute() {
        val before = DevelopmentRules(character(stealthRank = 4, dexterity = 4), catalog, DevelopmentProgress())
        assertFalse(before.availability(child).canIncrease)
        assertTrue(before.requirements(child).any { it.text.contains("Доступ к ветке") && it.status == RequirementStatus.FAIL })

        val ownedAccess = DevelopmentProgress().withRank(access, 1, 0)
        val after = DevelopmentRules(character(stealthRank = 4, dexterity = 4), catalog, ownedAccess)
        assertTrue(after.availability(child).canIncrease)
    }

    @Test
    fun alreadyOwnedEntryIsNotDeletedWhenRequirementLaterFails() {
        var progress = DevelopmentProgress().withRank(access, 1, 0)
        progress = progress.withRank(child, 1)
        val rules = DevelopmentRules(character(stealthRank = 4, dexterity = 2), catalog, progress)
        assertEquals(1, progress.rank(child.id))
        assertTrue(rules.requirements(child).any { it.status == RequirementStatus.FAIL })
    }

    @Test
    fun abilityPointsReturnWhenAbilityIsRemoved() {
        val owned = DevelopmentProgress().withRank(access, 1, 0)
        val spent = DevelopmentRules(character(stealthRank = 4), catalog, owned)
        assertEquals(1, spent.abilityPointsSpent())
        assertEquals(0, spent.abilityPointsAvailable())

        val removed = owned.withRank(access, 0)
        val restored = DevelopmentRules(character(stealthRank = 4), catalog, removed)
        assertEquals(0, restored.abilityPointsSpent())
        assertEquals(1, restored.abilityPointsAvailable())
    }
    @Test
    fun creationOnlyRequirementClosesAfterCreation() {
        val creationOnly = child.copy(
            id = "creation-only",
            name = "Природная красота",
            accessId = null,
            requirements = "Ловкость 4, только при создании",
        )
        val localCatalog = DevelopmentCatalog("test", listOf(creationOnly))

        val duringCreation = character(dexterity = 4).copy(creationComplete = false)
        val openRules = DevelopmentRules(duringCreation, localCatalog, DevelopmentProgress())
        assertTrue(openRules.availability(creationOnly).canIncrease)

        val completed = duringCreation.copy(creationComplete = true, creationExperience = duringCreation.experience)
        val closedRules = DevelopmentRules(completed, localCatalog, DevelopmentProgress())
        assertFalse(closedRules.availability(creationOnly).canIncrease)
        assertTrue(closedRules.requirements(creationOnly).any { it.text.contains("создание уже завершено") })

        val ownedProgress = DevelopmentProgress().withRank(creationOnly, 1)
        val ownedRules = DevelopmentRules(completed, localCatalog, ownedProgress)
        assertTrue(ownedRules.requirements(creationOnly).all { it.status == RequirementStatus.OK })
    }

    @Test
    fun abilityPointOverspendIsWarningStateNotHardPurchaseBlock() {
        val expensive = access.copy(abilityOptions = listOf(AbilityOption("Мастерство", 2)))
        val localCatalog = DevelopmentCatalog("test", listOf(expensive))
        val onePointCharacter = character(stealthRank = 4, xp = 1000)
        val localRules = DevelopmentRules(onePointCharacter, localCatalog, DevelopmentProgress())

        assertEquals(1, localRules.abilityPointsBudget())
        assertTrue(localRules.availability(expensive).canIncrease)

        val overspent = DevelopmentProgress().withRank(expensive, 1, 0)
        val spentRules = DevelopmentRules(onePointCharacter, localCatalog, overspent)
        assertEquals(-1, spentRules.abilityPointsAvailable())
    }

    @Test
    fun developmentEntriesSeparateRegularAndSpecialBranches() {
        val regular = child.copy(
            id = "regular",
            name = "Крепкий хват",
            section = "Навыки",
            category = "Общие",
            accessId = null,
        )
        val perfect = child.copy(
            id = "perfect",
            name = "Ассасин",
            section = "Ветки способностей",
            category = "Ассасин",
            accessId = null,
            perfectRoot = true,
        )

        assertTrue(regular.isRegularDevelopment)
        assertFalse(regular.isSpecialDevelopment)
        assertTrue(access.isSpecialDevelopment)
        assertTrue(child.isSpecialDevelopment)
        assertTrue(perfect.isSpecialDevelopment)
        assertFalse(perfect.isRegularDevelopment)
    }

    @Test
    fun orRequirementAcceptsBareCombatSkillShorthand() {
        val either = child.copy(
            id = "either-combat-skill",
            name = "Тест ИЛИ",
            accessId = null,
            requirements = "Рукопашный ИЛИ Холодное",
        )
        val localCatalog = DevelopmentCatalog("test", listOf(either))

        val unarmedCharacter = character().copy(
            skills = mapOf(
                "unarmed" to CharacterSkill(id = "unarmed", definitionId = "unarmed", rank = 1),
            ),
        )
        val unarmedRules = DevelopmentRules(unarmedCharacter, localCatalog, DevelopmentProgress())
        assertTrue(unarmedRules.availability(either).canIncrease)
        assertTrue(unarmedRules.requirements(either).all { it.status == RequirementStatus.OK })

        val meleeCharacter = character().copy(
            skills = mapOf(
                "melee_weapon" to CharacterSkill(id = "melee_weapon", definitionId = "melee_weapon", rank = 1),
            ),
        )
        val meleeRules = DevelopmentRules(meleeCharacter, localCatalog, DevelopmentProgress())
        assertTrue(meleeRules.availability(either).canIncrease)
        assertTrue(meleeRules.requirements(either).all { it.status == RequirementStatus.OK })

        val missingRules = DevelopmentRules(character(), localCatalog, DevelopmentProgress())
        assertFalse(missingRules.availability(either).canIncrease)
        assertTrue(missingRules.requirements(either).all { it.status != RequirementStatus.MANUAL })
    }

    @Test
    fun orRequirementPropagatesTrailingRankToEveryAlternative() {
        val either = child.copy(
            id = "either-ranked-combat-skill",
            name = "Тест ИЛИ ранга",
            accessId = null,
            requirements = "Холодное оружие или Рукопашный бой 4",
        )
        val localCatalog = DevelopmentCatalog("test", listOf(either))
        val unarmedCharacter = character().copy(
            skills = mapOf(
                "unarmed" to CharacterSkill(id = "unarmed", definitionId = "unarmed", rank = 4),
            ),
        )
        val rules = DevelopmentRules(unarmedCharacter, localCatalog, DevelopmentProgress())
        assertTrue(rules.availability(either).canIncrease)
        assertTrue(rules.requirements(either).all { it.status == RequirementStatus.OK })
    }

    @Test
    fun unmetRequirementsCanBeForcePurchasedButRemainInvalid() {
        val locked = child.copy(
            id = "forceable",
            name = "Принудительная покупка",
            accessId = null,
            requirements = "Ловкость 8",
        )
        val localCatalog = DevelopmentCatalog("test", listOf(locked))
        val baseCharacter = character(dexterity = 2)
        val before = DevelopmentRules(baseCharacter, localCatalog, DevelopmentProgress())

        assertFalse(before.availability(locked).canIncrease)
        assertTrue(before.availability(locked).canForceIncrease)

        val forcedProgress = DevelopmentProgress().withRank(locked, 1)
        val after = DevelopmentRules(baseCharacter, localCatalog, forcedProgress)
        assertEquals(1, forcedProgress.rank(locked.id))
        assertTrue(after.requirements(locked).any { it.status == RequirementStatus.FAIL })
        assertFalse(after.availability(locked).canIncrease)
        assertTrue(after.availability(locked).canForceIncrease)
    }

}
