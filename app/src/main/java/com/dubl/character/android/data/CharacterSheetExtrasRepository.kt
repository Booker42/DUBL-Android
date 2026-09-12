package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.QuickCheckId

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
        val favorites = prefs.getString("${prefix}favorites", "").orEmpty()
            .split(',')
            .filter { it.isNotBlank() }
            .mapNotNull { raw -> QuickCheckId.entries.firstOrNull { it.name == raw } }
            .distinct()
            .take(4)

        return CharacterSheetExtras(
            portraitUri = portraitUri,
            activeConditions = conditions,
            favoriteChecks = favorites,
        )
    }

    fun save(characterId: String, extras: CharacterSheetExtras) {
        val prefix = prefix(characterId)
        prefs.edit()
            .putString("${prefix}portrait", extras.portraitUri)
            .putStringSet("${prefix}conditions", extras.activeConditions.map { it.name }.toSet())
            .putString("${prefix}favorites", extras.favoriteChecks.joinToString(",") { it.name })
            .apply()
    }

    private fun prefix(characterId: String): String = "character.$characterId."
}
