package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.CharacterGear
import com.dubl.character.android.model.CharacterMagic
import com.dubl.character.android.model.CharacterSkill
import com.dubl.character.android.model.CustomResource
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.MagicSchool
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
        put("schema", 7)
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
        put("creationExperience", character.creationExperience)
        put("creationComplete", character.creationComplete)
        put("xpAdjustment", character.xpAdjustment)
        character.abilityPointsOverride?.let { put("abilityPointsOverride", it) }
        put("size", character.size)
        put("legs", character.legs)
        put("hpCurrent", character.hpCurrent)
        put("enduranceCurrent", character.enduranceCurrent)
        put("manaEnabled", character.manaEnabled)
        put("manaCurrent", character.manaCurrent)
        put("manaMaximum", character.manaMaximum)
        put("chiEnabled", character.chiEnabled)
        put("chiCurrent", character.chiCurrent)
        put("chiBonusRanks", character.chiBonusRanks)
        character.healthMaximumOverride?.let { put("healthMaximumOverride", it) }
        character.enduranceMaximumOverride?.let { put("enduranceMaximumOverride", it) }
        character.manaMaximumOverride?.let { put("manaMaximumOverride", it) }
        put("customResources", JSONArray().apply {
            character.customResources.forEach { resource ->
                put(JSONObject().apply {
                    put("uid", resource.uid)
                    put("name", resource.name)
                    put("current", resource.current)
                    put("maximum", resource.maximum)
                })
            }
        })
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
        put("magic", encodeMagic(character.magic))
        put("gear", encodeGear(character.gear))
    }


    private fun encodeMagic(magic: CharacterMagic): JSONObject = JSONObject().apply {
        put("manaRank", magic.manaRank)
        put("power", magic.power)
        put("schools", JSONArray().apply {
            magic.schools.forEach { school ->
                put(JSONObject().apply {
                    put("name", school.name)
                    put("rank", school.rank)
                    put("note", school.note)
                })
            }
        })
        put("spells", JSONArray().apply {
            magic.spells.forEach { spell ->
                put(JSONObject().apply {
                    put("uid", spell.uid)
                    spell.catalogId?.let { put("catalogId", it) }
                    put("name", spell.name)
                    put("school", spell.school)
                    put("cost", spell.cost)
                    put("manaText", spell.manaText)
                    put("time", spell.time)
                    put("range", spell.range)
                    put("area", spell.area)
                    put("action", spell.action)
                    put("duration", spell.duration)
                    put("description", spell.description)
                    put("enhancement", spell.enhancement)
                    put("learned", spell.learned)
                    spell.xpOverride?.let { put("xpOverride", it) }
                    put("incomplete", spell.incomplete)
                    put("conflictNote", spell.conflictNote)
                    put("custom", spell.custom)
                })
            }
        })
    }

    private fun encodeGear(gear: CharacterGear): JSONObject = JSONObject().apply {
        put("loadAutomatic", gear.loadAutomatic)
        put("loadManual", gear.loadManual)
        put("items", JSONArray().apply {
            gear.items.forEach { item ->
                put(JSONObject().apply {
                    put("uid", item.uid)
                    item.catalogId?.let { put("catalogId", it) }
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("load", item.load)
                    put("carried", item.carried)
                    put("description", item.description)
                    put("category", item.category)
                    put("section", item.section)
                    put("custom", item.custom)
                    put("fields", JSONObject().apply {
                        item.fields.forEach { (key, value) -> put(key, value) }
                    })
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
        val schema = root.optInt("schema", 1)
        val array = root.optJSONArray("characters") ?: JSONArray()
        val characters = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(decodeCharacter(item, schema))
            }
        }
        if (characters.isEmpty()) return freshSnapshot()

        val requestedActive = root.optString("activeCharacterId")
        val active = characters.firstOrNull { it.id == requestedActive }?.id ?: characters.first().id
        return AppSnapshot(characters, active)
    }

    private fun decodeCharacter(root: JSONObject, schema: Int): DublCharacter {
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

        val magic = decodeMagic(root.optJSONObject("magic"))
        val gear = decodeGear(root.optJSONObject("gear"))
        val customResources = buildList {
            val array = root.optJSONArray("customResources") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uid = item.optString("uid").ifBlank { UUID.randomUUID().toString() }
                add(
                    CustomResource(
                        uid = uid,
                        name = item.optString("name", "Ресурс"),
                        current = item.optInt("current", 0),
                        maximum = item.optInt("maximum", 0),
                    )
                )
            }
        }

        val experience = root.optInt("experience", 0).coerceAtLeast(0)
        val creationExperience = if (root.has("creationExperience")) {
            root.optInt("creationExperience", experience).coerceAtLeast(0)
        } else {
            experience
        }
        val creationComplete = if (root.has("creationComplete")) {
            root.optBoolean("creationComplete", false)
        } else {
            // Existing pre-v5 characters are treated as already created so migration
            // never grants creation-only purchases or auto-restores resources.
            schema < 5
        }

        return DublCharacter(
            id = root.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = root.optString("name", "Новый персонаж"),
            concept = root.optString("concept", ""),
            experience = experience,
            creationExperience = creationExperience,
            creationComplete = creationComplete,
            xpAdjustment = root.optInt("xpAdjustment", 0),
            abilityPointsOverride = if (root.has("abilityPointsOverride") && !root.isNull("abilityPointsOverride")) {
                root.optInt("abilityPointsOverride").coerceAtLeast(0)
            } else null,
            size = root.optInt("size", 5),
            legs = root.optInt("legs", 2),
            attributes = attributes,
            hpCurrent = root.optInt("hpCurrent", 0),
            enduranceCurrent = root.optInt("enduranceCurrent", 3),
            manaEnabled = root.optBoolean("manaEnabled", false),
            manaCurrent = root.optInt("manaCurrent", 0),
            manaMaximum = root.optInt("manaMaximum", 0),
            chiEnabled = root.optBoolean("chiEnabled", false),
            chiCurrent = root.optInt("chiCurrent", 0),
            chiBonusRanks = root.optInt("chiBonusRanks", 0),
            healthMaximumOverride = if (root.has("healthMaximumOverride") && !root.isNull("healthMaximumOverride")) root.optInt("healthMaximumOverride") else null,
            enduranceMaximumOverride = if (root.has("enduranceMaximumOverride") && !root.isNull("enduranceMaximumOverride")) root.optInt("enduranceMaximumOverride") else null,
            manaMaximumOverride = if (root.has("manaMaximumOverride") && !root.isNull("manaMaximumOverride")) root.optInt("manaMaximumOverride") else null,
            customResources = customResources,
            skills = skills,
            hiddenSkillIds = hidden,
            development = development,
            magic = magic,
            gear = gear,
        ).normalized()
    }


    private fun decodeMagic(root: JSONObject?): CharacterMagic {
        if (root == null) return CharacterMagic()
        val schools = buildList {
            val array = root.optJSONArray("schools") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    MagicSchool(
                        name = item.optString("name", "Школа"),
                        rank = item.optInt("rank", item.optInt("level", 0)),
                        note = item.optString("note", ""),
                    )
                )
            }
        }
        val spells = buildList {
            val array = root.optJSONArray("spells") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val uid = item.optString("uid").ifBlank { UUID.randomUUID().toString() }
                add(
                    KnownSpell(
                        uid = uid,
                        catalogId = item.optString("catalogId").takeIf { it.isNotBlank() },
                        name = item.optString("name", "Заклинание"),
                        school = item.optString("school", ""),
                        cost = item.optInt("cost", 0),
                        manaText = item.optString("manaText", ""),
                        time = item.optString("time", ""),
                        range = item.optString("range", ""),
                        area = item.optString("area", ""),
                        action = item.optString("action", ""),
                        duration = item.optString("duration", ""),
                        description = item.optString("description", ""),
                        enhancement = item.optString("enhancement", ""),
                        learned = item.optBoolean("learned", true),
                        xpOverride = if (item.has("xpOverride") && !item.isNull("xpOverride")) item.optInt("xpOverride") else null,
                        incomplete = item.optBoolean("incomplete", false),
                        conflictNote = item.optString("conflictNote", ""),
                        custom = item.optBoolean("custom", false),
                    )
                )
            }
        }
        return CharacterMagic(
            manaRank = root.optInt("manaRank", 0),
            power = root.optInt("power", 0),
            schools = schools,
            spells = spells,
        )
    }

    private fun decodeGear(root: JSONObject?): CharacterGear {
        if (root == null) return CharacterGear()
        val items = buildList {
            val array = root.optJSONArray("items") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val fieldsRoot = item.optJSONObject("fields")
                val fields = linkedMapOf<String, String>()
                if (fieldsRoot != null) {
                    val keys = fieldsRoot.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        fields[key] = fieldsRoot.optString(key, "")
                    }
                }
                add(
                    GearItem(
                        uid = item.optString("uid").ifBlank { UUID.randomUUID().toString() },
                        catalogId = item.optString("catalogId").takeIf { it.isNotBlank() },
                        name = item.optString("name", "Предмет"),
                        quantity = item.optInt("quantity", item.optInt("qty", 1)),
                        load = item.optDouble("load", 0.0),
                        carried = item.optBoolean("carried", true),
                        description = item.optString("description", ""),
                        category = item.optString("category", "Снаряжение"),
                        section = item.optString("section", "Предметы"),
                        fields = fields,
                        custom = item.optBoolean("custom", false),
                    )
                )
            }
        }
        return CharacterGear(
            loadAutomatic = root.optBoolean("loadAutomatic", true),
            loadManual = root.optDouble("loadManual", 0.0),
            items = items,
        )
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
