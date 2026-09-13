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

}
