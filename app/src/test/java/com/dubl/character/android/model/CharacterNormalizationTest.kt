package com.dubl.character.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterNormalizationTest {
    @Test
    fun normalizationClampsNewSystemsWithoutDroppingCharacterData() {
        val character = DublCharacter(
            id = "normalize",
            hpCurrent = 999,
            manaCurrent = 999,
            magic = CharacterMagic(
                manaRank = 9,
                power = -3,
                schools = listOf(MagicSchool(name = " ", rank = -2, note = " note ")),
                spells = listOf(
                    KnownSpell(uid = "spell", name = " ", cost = -5, xpOverride = -10),
                    KnownSpell(uid = "spell", name = "duplicate"),
                ),
            ),
            gear = CharacterGear(
                loadAutomatic = false,
                loadManual = -4.0,
                items = listOf(
                    GearItem(uid = "gear", name = " ", quantity = -3, load = -2.0),
                    GearItem(uid = "gear", name = "duplicate"),
                ),
            ),
        )

        val normalized = character.normalized()

        assertEquals(5, normalized.magic.manaRank)
        assertEquals(1, normalized.magic.power)
        assertEquals("Школа", normalized.magic.schools.single().name)
        assertEquals(0, normalized.magic.schools.single().rank)
        assertEquals("note", normalized.magic.schools.single().note)
        assertEquals(1, normalized.magic.spells.size)
        assertEquals("Заклинание", normalized.magic.spells.single().name)
        assertEquals(0, normalized.magic.spells.single().cost)
        assertEquals(0, normalized.magic.spells.single().xpOverride)
        assertEquals(1, normalized.gear.items.size)
        assertEquals("Предмет", normalized.gear.items.single().name)
        assertEquals(1, normalized.gear.items.single().quantity)
        assertEquals(0.0, normalized.gear.items.single().load, 0.001)
        assertEquals(0.0, normalized.gear.loadManual, 0.001)
        assertTrue(normalized.manaEnabled)
        assertTrue(normalized.manaCurrent <= normalized.effectiveManaMaximum)
    }

    @Test
    fun legacyCharacterDefaultsRemainSafe() {
        val character = DublCharacter(id = "legacy").normalized()
        assertTrue(character.magic.schools.isEmpty())
        assertTrue(character.magic.spells.isEmpty())
        assertTrue(character.gear.items.isEmpty())
        assertFalse(character.manaEnabled)
        assertEquals(0, character.manaCurrent)
    }
}
