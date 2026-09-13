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
}
