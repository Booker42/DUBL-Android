package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.defaultAttributes
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CharacterRepository(context: Context) {
    private val preferences = context.getSharedPreferences("dubl_native_state", Context.MODE_PRIVATE)

    fun load(): AppSnapshot {
        val raw = preferences.getString(KEY_STATE, null) ?: return freshSnapshot()
        return runCatching { decodeSnapshot(JSONObject(raw)) }
            .getOrElse { freshSnapshot() }
    }

    fun save(snapshot: AppSnapshot) {
        preferences.edit().putString(KEY_STATE, encodeSnapshot(snapshot).toString()).apply()
    }

    fun newCharacter(): DublCharacter = DublCharacter(
        id = UUID.randomUUID().toString(),
        name = "Новый персонаж",
    )

    private fun freshSnapshot(): AppSnapshot {
        val character = newCharacter()
        return AppSnapshot(listOf(character), character.id)
    }

    private fun encodeSnapshot(snapshot: AppSnapshot): JSONObject = JSONObject().apply {
        put("schema", 1)
        put("activeCharacterId", snapshot.activeCharacterId)
        put("characters", JSONArray().apply {
            snapshot.characters.forEach { put(encodeCharacter(it)) }
        })
    }

    private fun encodeCharacter(character: DublCharacter): JSONObject = JSONObject().apply {
        put("id", character.id)
        put("name", character.name)
        put("concept", character.concept)
        put("experience", character.experience)
        put("size", character.size)
        put("legs", character.legs)
        put("hpCurrent", character.hpCurrent)
        put("enduranceCurrent", character.enduranceCurrent)
        put("manaEnabled", character.manaEnabled)
        put("manaCurrent", character.manaCurrent)
        put("manaMaximum", character.manaMaximum)
        put("attributes", JSONObject().apply {
            character.attributes.forEach { (id, value) ->
                put(id.name, JSONObject().apply {
                    put("base", value.base)
                    put("bonus", value.bonus)
                })
            }
        })
    }

    private fun decodeSnapshot(root: JSONObject): AppSnapshot {
        val array = root.optJSONArray("characters") ?: JSONArray()
        val characters = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(decodeCharacter(item))
            }
        }
        if (characters.isEmpty()) return freshSnapshot()

        val requestedActive = root.optString("activeCharacterId")
        val active = characters.firstOrNull { it.id == requestedActive }?.id ?: characters.first().id
        return AppSnapshot(characters, active)
    }

    private fun decodeCharacter(root: JSONObject): DublCharacter {
        val attributes = defaultAttributes().toMutableMap()
        val jsonAttributes = root.optJSONObject("attributes")
        AttributeId.entries.forEach { id ->
            val value = jsonAttributes?.optJSONObject(id.name)
            if (value != null) {
                attributes[id] = AttributeValue(
                    base = value.optInt("base", 0),
                    bonus = value.optInt("bonus", 0),
                )
            }
        }

        return DublCharacter(
            id = root.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = root.optString("name", "Новый персонаж"),
            concept = root.optString("concept", ""),
            experience = root.optInt("experience", 0),
            size = root.optInt("size", 5),
            legs = root.optInt("legs", 2),
            attributes = attributes,
            hpCurrent = root.optInt("hpCurrent", 0),
            enduranceCurrent = root.optInt("enduranceCurrent", 3),
            manaEnabled = root.optBoolean("manaEnabled", false),
            manaCurrent = root.optInt("manaCurrent", 0),
            manaMaximum = root.optInt("manaMaximum", 0),
        ).normalized()
    }

    private companion object {
        const val KEY_STATE = "state_v1"
    }
}
