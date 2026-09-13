package com.dubl.character.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dubl.character.android.data.CharacterRepository
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterSkill
import com.dubl.character.android.model.GearCatalogEntry
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.MagicEquipmentRules
import com.dubl.character.android.model.MagicSchool
import com.dubl.character.android.model.SpellCatalogEntry
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.OwnedDevelopment
import com.dubl.character.android.model.SkillCatalog
import com.dubl.character.android.model.UntrainedRule
import com.dubl.character.android.model.resolvedSkills
import java.util.UUID

class CharacterController(private val repository: CharacterRepository) {
    var snapshot by mutableStateOf(repository.load())
        private set

    val active: DublCharacter get() = snapshot.activeCharacter

    fun updateActive(transform: (DublCharacter) -> DublCharacter) {
        val activeId = snapshot.activeCharacterId
        val updated = snapshot.characters.map { character ->
            if (character.id == activeId) transform(character).normalized() else character
        }
        persist(snapshot.copy(characters = updated))
    }

    fun changeAttribute(id: AttributeId, delta: Int) = updateActive { character ->
        val current = character.attributes.getValue(id)
        character.copy(attributes = character.attributes + (id to current.copy(base = current.base + delta)))
    }

    fun changeHp(delta: Int) = updateActive { it.copy(hpCurrent = it.hpCurrent + delta) }
    fun changeEndurance(delta: Int) = updateActive { it.copy(enduranceCurrent = it.enduranceCurrent + delta) }
    fun changeMana(delta: Int) = updateActive { it.copy(manaCurrent = it.manaCurrent + delta) }

    fun changeSkillRank(skillId: String, delta: Int) = updateSkill(skillId) { skill ->
        skill.copy(rank = (skill.rank + delta).coerceIn(0, 10))
    }

    fun setSkillAttributes(skillId: String, attributes: List<AttributeId>) {
        val clean = attributes.distinct()
        if (clean.isEmpty()) return
        updateSkill(skillId) { it.copy(attributes = clean) }
    }

    fun setSkillModifier(skillId: String, modifier: Int) = updateSkill(skillId) {
        it.copy(modifier = modifier.coerceIn(-99, 99))
    }

    fun setSkillFormulaNote(skillId: String, note: String) = updateSkill(skillId) {
        it.copy(formulaNote = note.trim())
    }

    fun hideSkill(skillId: String) = updateActive { character ->
        character.copy(hiddenSkillIds = character.hiddenSkillIds + skillId)
    }

    fun restoreSkill(skillId: String) = updateActive { character ->
        character.copy(hiddenSkillIds = character.hiddenSkillIds - skillId)
    }

    fun restoreAllSkills() = updateActive { it.copy(hiddenSkillIds = emptySet()) }

    fun setDevelopmentRank(entryId: String, rank: Int, optionIndex: Int = 0) = updateActive { character ->
        val next = character.development.toMutableMap()
        if (rank <= 0) {
            next.remove(entryId)
        } else {
            next[entryId] = OwnedDevelopment(
                rank = rank,
                optionIndex = optionIndex.coerceAtLeast(0),
            )
        }
        character.copy(development = next)
    }


    fun setMagicManaRank(rank: Int) = updateActive { character ->
        val nextRank = rank.coerceIn(0, 5)
        val nextPower = if (nextRank > 0) character.magic.power.coerceAtLeast(1) else character.magic.power
        character.copy(
            magic = character.magic.copy(manaRank = nextRank, power = nextPower),
            development = character.development - MagicEquipmentRules.BASE_MANA_ENTRY_ID,
            manaEnabled = if (nextRank > 0) true else false,
            manaCurrent = if (nextRank > 0) character.manaCurrent else 0,
            manaMaximum = if (nextRank > 0) character.manaMaximum else 0,
        )
    }

    fun setMagicPower(power: Int) = updateActive { character ->
        val nextPower = if (character.magic.manaRank > 0) power.coerceAtLeast(1) else power.coerceAtLeast(0)
        character.copy(magic = character.magic.copy(power = nextPower))
    }

    fun addMagicSchool(name: String, rank: Int, note: String): Boolean {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return false
        if (active.magic.schools.any { it.name.equals(clean, ignoreCase = true) }) return false
        updateActive { character ->
            character.copy(
                magic = character.magic.copy(
                    schools = character.magic.schools + MagicSchool(clean, rank.coerceAtLeast(0), note.trim()),
                ),
            )
        }
        return true
    }

    fun updateMagicSchool(index: Int, name: String, rank: Int, note: String): Boolean {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank() || index !in active.magic.schools.indices) return false
        if (active.magic.schools.withIndex().any { (otherIndex, school) ->
                otherIndex != index && school.name.equals(clean, ignoreCase = true)
            }) return false
        updateActive { character ->
            if (index !in character.magic.schools.indices) return@updateActive character
            val next = character.magic.schools.toMutableList()
            next[index] = next[index].copy(
                name = clean,
                rank = rank.coerceAtLeast(0),
                note = note.trim(),
            )
            character.copy(magic = character.magic.copy(schools = next))
        }
        return true
    }


    fun removeMagicSchool(index: Int) = updateActive { character ->
        if (index !in character.magic.schools.indices) return@updateActive character
        character.copy(magic = character.magic.copy(schools = character.magic.schools.filterIndexed { i, _ -> i != index }))
    }

    fun addCatalogSpell(entry: SpellCatalogEntry): Boolean {
        if (active.magic.spells.any {
                it.catalogId == entry.id || it.name.equals(entry.name, ignoreCase = true)
            }) return false
        val spell = KnownSpell(
            uid = UUID.randomUUID().toString(),
            catalogId = entry.id,
            name = entry.name,
            school = entry.school,
            cost = entry.cost,
            manaText = entry.manaText,
            time = entry.time,
            range = entry.range,
            area = entry.area,
            action = entry.action,
            duration = entry.duration,
            description = entry.description,
            enhancement = entry.enhancement,
            learned = true,
            incomplete = entry.incomplete,
            conflictNote = entry.conflictNote,
        )
        updateActive { character -> character.copy(magic = character.magic.copy(spells = character.magic.spells + spell)) }
        return true
    }

    fun addCustomSpell(spell: KnownSpell): String {
        val uid = spell.uid.ifBlank { UUID.randomUUID().toString() }
        updateActive { character ->
            character.copy(
                magic = character.magic.copy(
                    spells = character.magic.spells + spell.copy(uid = uid, catalogId = null, custom = true),
                ),
            )
        }
        return uid
    }

    fun updateSpell(uid: String, transform: (KnownSpell) -> KnownSpell) = updateActive { character ->
        character.copy(
            magic = character.magic.copy(
                spells = character.magic.spells.map { if (it.uid == uid) transform(it).copy(uid = uid) else it },
            ),
        )
    }

    fun removeSpell(uid: String) = updateActive { character ->
        character.copy(magic = character.magic.copy(spells = character.magic.spells.filterNot { it.uid == uid }))
    }

    fun setGearLoadAutomatic(enabled: Boolean) = updateActive { character ->
        character.copy(gear = character.gear.copy(loadAutomatic = enabled))
    }

    fun setGearManualLoad(value: Double) = updateActive { character ->
        character.copy(gear = character.gear.copy(loadManual = value.coerceAtLeast(0.0)))
    }

    fun syncCatalogGearLoads(entries: List<GearCatalogEntry>) {
        val byId = entries.associateBy { it.id }
        val current = active
        val repaired = current.gear.items.map { item ->
            val catalogEntry = item.catalogId?.let(byId::get) ?: return@map item
            val expected = MagicEquipmentRules.catalogGearLoad(catalogEntry)
            val legacyRequirementLoad = (catalogEntry.fields["Треб."] ?: catalogEntry.fields["Требование"] ?: "0")
                .replace(',', '.')
                .toDoubleOrNull()
                ?.coerceAtLeast(0.0)
                ?: 0.0
            val isLegacyCatalogValue = kotlin.math.abs(item.load - legacyRequirementLoad) < 0.0001
            if (!isLegacyCatalogValue || kotlin.math.abs(item.load - expected) < 0.0001) item
            else item.copy(load = expected)
        }
        if (repaired == current.gear.items) return
        updateActive { character -> character.copy(gear = character.gear.copy(items = repaired)) }
    }

    fun addCatalogGear(entry: GearCatalogEntry): String {
        val existing = active.gear.items.firstOrNull { it.catalogId == entry.id }
        if (existing != null) {
            updateGearItem(existing.uid) { item -> item.copy(quantity = item.quantity + 1) }
            return existing.uid
        }

        val uid = UUID.randomUUID().toString()
        val load = MagicEquipmentRules.catalogGearLoad(entry)
        val item = GearItem(
            uid = uid,
            catalogId = entry.id,
            name = entry.name,
            quantity = 1,
            load = load.coerceAtLeast(0.0),
            carried = true,
            description = entry.description,
            category = entry.category,
            section = entry.section,
            fields = entry.fields,
        )
        updateActive { character -> character.copy(gear = character.gear.copy(items = character.gear.items + item)) }
        return uid
    }

    fun addCustomGear(item: GearItem): String {
        val uid = item.uid.ifBlank { UUID.randomUUID().toString() }
        updateActive { character ->
            character.copy(
                gear = character.gear.copy(items = character.gear.items + item.copy(uid = uid, catalogId = null, custom = true)),
            )
        }
        return uid
    }

    fun updateGearItem(uid: String, transform: (GearItem) -> GearItem) = updateActive { character ->
        character.copy(
            gear = character.gear.copy(
                items = character.gear.items.map { if (it.uid == uid) transform(it).copy(uid = uid) else it },
            ),
        )
    }

    fun removeGearItem(uid: String) = updateActive { character ->
        character.copy(gear = character.gear.copy(items = character.gear.items.filterNot { it.uid == uid }))
    }

    fun addSpecializedSkill(templateId: String, specialization: String): String? {
        val template = SkillCatalog.templates.firstOrNull { it.id == templateId } ?: return null
        val clean = specialization.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return null
        val displayName = when (template.id) {
            "knowledge_template" -> "Знание ($clean)"
            "performance_template" -> "Исполнение ($clean)"
            "profession_template" -> "Профессия ($clean)"
            "craft_template" -> "Ремесло ($clean)"
            else -> "${template.name} ($clean)"
        }
        if (active.resolvedSkills(includeHidden = true).any { it.name.equals(displayName, ignoreCase = true) }) {
            return null
        }
        val id = UUID.randomUUID().toString()
        updateActive { character ->
            character.copy(
                skills = character.skills + (
                    id to CharacterSkill(
                        id = id,
                        definitionId = template.id,
                        name = displayName,
                        attributes = listOf(template.defaultAttribute),
                    )
                ),
            )
        }
        return id
    }

    fun addCustomSkill(
        name: String,
        description: String,
        attributes: List<AttributeId>,
        untrained: UntrainedRule,
    ): String? {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val cleanAttrs = attributes.distinct()
        if (cleanName.isBlank() || cleanAttrs.isEmpty()) return null
        if (active.resolvedSkills(includeHidden = true).any { it.name.equals(cleanName, ignoreCase = true) }) {
            return null
        }
        val id = UUID.randomUUID().toString()
        updateActive { character ->
            character.copy(
                skills = character.skills + (
                    id to CharacterSkill(
                        id = id,
                        name = cleanName,
                        description = description.trim(),
                        attributes = cleanAttrs,
                        untrainedOverride = untrained,
                    )
                ),
            )
        }
        return id
    }

    fun deleteDynamicSkill(skillId: String) {
        if (SkillCatalog.builtIns.any { it.id == skillId }) return
        updateActive { character ->
            character.copy(
                skills = character.skills - skillId,
                hiddenSkillIds = character.hiddenSkillIds - skillId,
            )
        }
    }

    fun createCharacter() {
        val created = repository.newCharacter()
        persist(
            AppSnapshot(
                characters = snapshot.characters + created,
                activeCharacterId = created.id,
            )
        )
    }

    fun selectCharacter(id: String) {
        if (snapshot.characters.any { it.id == id }) {
            persist(snapshot.copy(activeCharacterId = id))
        }
    }

    fun deleteActive() {
        if (snapshot.characters.size <= 1) return
        val remaining = snapshot.characters.filterNot { it.id == snapshot.activeCharacterId }
        persist(AppSnapshot(remaining, remaining.first().id))
    }

    private fun updateSkill(skillId: String, transform: (CharacterSkill) -> CharacterSkill) {
        updateActive { character ->
            val existing = character.skills[skillId]
            val builtIn = SkillCatalog.builtIns.firstOrNull { it.id == skillId }
            val base = existing ?: builtIn?.let {
                CharacterSkill(id = it.id, definitionId = it.id)
            } ?: return@updateActive character
            val updated = transform(base).copy(id = skillId)
            character.copy(skills = character.skills + (skillId to updated))
        }
    }

    private fun persist(newSnapshot: AppSnapshot) {
        snapshot = newSnapshot
        repository.save(newSnapshot)
    }
}
