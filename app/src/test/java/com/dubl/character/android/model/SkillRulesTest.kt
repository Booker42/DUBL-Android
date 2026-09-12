package com.dubl.character.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkillRulesTest {
    private fun character(
        vararg values: Pair<AttributeId, Int>,
        skills: Map<String, CharacterSkill> = emptyMap(),
    ): DublCharacter {
        val attrs = defaultAttributes().toMutableMap()
        values.forEach { (id, value) -> attrs[id] = AttributeValue(base = value) }
        return DublCharacter(id = "test", attributes = attrs, skills = skills)
    }

    @Test
    fun multiAttributeSkillAddsEverySelectedAttributeOnce() {
        val state = CharacterSkill(
            id = "athletics",
            definitionId = "athletics",
            rank = 2,
            attributes = listOf(AttributeId.STRENGTH, AttributeId.DEXTERITY),
        )
        val character = character(
            AttributeId.STRENGTH to 3,
            AttributeId.DEXTERITY to 4,
            skills = mapOf("athletics" to state),
        )
        val skill = character.resolveSkill("athletics")!!

        assertEquals(9, character.skillCalculation(skill).total)
    }

    @Test
    fun untrainedMinusTwoPenaltyIsAppliedAtRankZero() {
        val character = character(AttributeId.DEXTERITY to 4)
        val skill = character.resolveSkill("riding")!!

        assertEquals(2, character.skillCalculation(skill).total)
    }

    @Test
    fun untrainedForbiddenSkillHasNoTotalAtRankZero() {
        val character = character(AttributeId.INTELLIGENCE to 5)
        val skill = character.resolveSkill("engineering_repair")!!
        val result = character.skillCalculation(skill)

        assertNull(result.total)
        assertEquals("Нельзя использовать без обучения", result.unavailableReason)
    }

    @Test
    fun rankCostsMatchDesktopCatalog() {
        assertEquals(listOf(0, 10, 30, 60, 100, 150, 210, 280, 360, 450, 550), SkillCatalog.rankCosts)
        assertEquals(50, SkillCatalog.nextRankCost(4))
        assertNull(SkillCatalog.nextRankCost(10))
    }
}
