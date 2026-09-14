package com.dubl.character.android.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetGroupingRulesTest {
    @Test
    fun movingItemDirectlyToAnotherGroupPreservesEveryOtherItem() {
        val groups = listOf(
            SheetGroup("combat", "Бой", listOf("parry", "block")),
            SheetGroup("travel", "Дорога", listOf("survival")),
        )

        val moved = SheetGroupingRules.moveItem(
            groups = groups,
            itemId = "block",
            targetGroupId = "travel",
            targetIndex = 1,
        )

        assertEquals(listOf("parry"), moved[0].itemIds)
        assertEquals(listOf("survival", "block"), moved[1].itemIds)
    }
}
