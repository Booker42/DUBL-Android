package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.CharacterSkill
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.OwnedDevelopment
import com.dubl.character.android.model.UntrainedRule
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
        put("schema", 3)
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
        put("skills", JSONArray().apply {
            character.skills.values.forEach { put(encodeSkill(it)) }
        })
        put("hiddenSkillIds", JSONArray().apply {
            character.hiddenSkillIds.forEach { put(it) }
        })
        put("development", JSONArray().apply {
            character.development.forEach { (id, owned) ->
                put(JSONObject().apply {
                    put("id", id)
                    put("rank", owned.rank)
                    put("option", owned.optionIndex)
                })
            }
        })
    }

    private fun encodeSkill(skill: CharacterSkill): JSONObject = JSONObject().apply {
        put("id", skill.id)
        skill.definitionId?.let { put("definitionId", it) }
        put("name", skill.name)
        put("description", skill.description)
        put("rank", skill.rank)
        put("attributes", JSONArray().apply { skill.attributes.forEach { put(it.name) } })
        put("modifier", skill.modifier)
        put("formulaNote", skill.formulaNote)
        skill.untrainedOverride?.let { put("untrainedOverride", it.name) }
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

        val skills = linkedMapOf<String, CharacterSkill>()
        val jsonSkills = root.optJSONArray("skills") ?: JSONArray()
        for (index in 0 until jsonSkills.length()) {
            val item = jsonSkills.optJSONObject(index) ?: continue
            val skill = decodeSkill(item) ?: continue
            skills[skill.id] = skill
        }

        val hidden = linkedSetOf<String>()
        val hiddenArray = root.optJSONArray("hiddenSkillIds") ?: JSONArray()
        for (index in 0 until hiddenArray.length()) {
            hiddenArray.optString(index).takeIf { it.isNotBlank() }?.let(hidden::add)
        }

        val development = linkedMapOf<String, OwnedDevelopment>()
        val developmentArray = root.optJSONArray("development") ?: JSONArray()
        for (index in 0 until developmentArray.length()) {
            val item = developmentArray.optJSONObject(index) ?: continue
            val id = item.optString("id").trim()
            val rank = item.optInt("rank", 0)
            if (id.isBlank() || rank <= 0) continue
            development[id] = OwnedDevelopment(
                rank = rank,
                optionIndex = item.optInt("option", 0).coerceAtLeast(0),
            )
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
            skills = skills,
            hiddenSkillIds = hidden,
            development = development,
        ).normalized()
    }

    private fun decodeSkill(root: JSONObject): CharacterSkill? {
        val id = root.optString("id").ifBlank { return null }
        val attrArray = root.optJSONArray("attributes") ?: JSONArray()
        val attrs = buildList {
            for (index in 0 until attrArray.length()) {
                val raw = attrArray.optString(index)
                AttributeId.entries.firstOrNull { it.name == raw }?.let { if (it !in this) add(it) }
            }
        }
        val untrained = root.optString("untrainedOverride").takeIf { it.isNotBlank() }
            ?.let { raw -> UntrainedRule.entries.firstOrNull { it.name == raw } }
        return CharacterSkill(
            id = id,
            definitionId = root.optString("definitionId").takeIf { it.isNotBlank() },
            name = root.optString("name", ""),
            description = root.optString("description", ""),
            rank = root.optInt("rank", 0),
            attributes = attrs,
            modifier = root.optInt("modifier", 0),
            formulaNote = root.optString("formulaNote", ""),
            untrainedOverride = untrained,
        )
    }

    private companion object {
        const val KEY_STATE = "state_v1"
    }
}
