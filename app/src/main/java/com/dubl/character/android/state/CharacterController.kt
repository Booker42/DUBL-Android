package com.dubl.character.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dubl.character.android.data.CharacterRepository
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterSkill
import com.dubl.character.android.model.DublCharacter
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
