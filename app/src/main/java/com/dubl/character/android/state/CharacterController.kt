package com.dubl.character.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dubl.character.android.data.CharacterRepository
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DublCharacter

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

    private fun persist(newSnapshot: AppSnapshot) {
        snapshot = newSnapshot
        repository.save(newSnapshot)
    }
}
