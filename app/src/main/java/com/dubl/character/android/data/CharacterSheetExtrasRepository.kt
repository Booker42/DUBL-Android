package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.CharacterSheetResourceId

class CharacterSheetExtrasRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "dubl_character_sheet_extras",
        Context.MODE_PRIVATE,
    )

    fun load(characterId: String): CharacterSheetExtras {
        val prefix = prefix(characterId)
        val portraitUri = prefs.getString("${prefix}portrait", null)
        val conditions = prefs.getStringSet("${prefix}conditions", emptySet()).orEmpty()
            .mapNotNull { raw -> CharacterConditionId.entries.firstOrNull { it.name == raw } }
            .toSet()
        val favoriteSkillIds = prefs.getString("${prefix}favorite_skill_ids", "").orEmpty()
            .split(FAVORITE_SEPARATOR)
            .filter { it.isNotBlank() }
            .distinct()
        val hiddenResourceIds = prefs.getStringSet("${prefix}hidden_resources", emptySet()).orEmpty()
            .mapNotNull { raw -> CharacterSheetResourceId.entries.firstOrNull { it.name == raw } }
            .toSet()

        return CharacterSheetExtras(
            portraitUri = portraitUri,
            activeConditions = conditions,
            favoriteSkillIds = favoriteSkillIds,
            hiddenResourceIds = hiddenResourceIds,
        )
    }

    fun save(characterId: String, extras: CharacterSheetExtras) {
        val prefix = prefix(characterId)
        prefs.edit()
            .putString("${prefix}portrait", extras.portraitUri)
            .putStringSet("${prefix}conditions", extras.activeConditions.map { it.name }.toSet())
            .putString("${prefix}favorite_skill_ids", extras.favoriteSkillIds.joinToString(FAVORITE_SEPARATOR))
            .putStringSet("${prefix}hidden_resources", extras.hiddenResourceIds.map { it.name }.toSet())
            .remove("${prefix}favorites") // v4 pre-skill favorites were stats/attributes and are intentionally discarded.
            .apply()
    }

    private fun prefix(characterId: String): String = "character.$characterId."

    private companion object {
        const val FAVORITE_SEPARATOR = "|"
    }
}
