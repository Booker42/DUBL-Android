package com.dubl.character.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.components.StatGrid
import java.text.DecimalFormat

@Composable
fun OverviewScreen(controller: CharacterController) {
    val character = controller.active
    var showEdit by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CharacterHeader(
                character = character,
                onEdit = { showEdit = true },
            )
        }

        item {
            Resources(character, controller)
        }

        item {
            Text("Второстепенные показатели", style = MaterialTheme.typography.titleLarge)
        }

        item {
            StatGrid(
                listOf(
                    "Защита" to character.defense.toString(),
                    "Рефлексы" to character.reflexes.toString(),
                    "Инициатива" to character.initiative.toString(),
                    "Стойкость" to character.fortitude.toString(),
                    "Бег" to formatNumber(character.runFull),
                    "Очки способностей" to character.abilityPoints.toString(),
                )
            )
        }

        item {
            Text("Характеристики", style = MaterialTheme.typography.titleLarge)
        }

        item {
            AttributeGrid(character, controller)
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showEdit) {
        EditCharacterDialog(
            character = character,
            onDismiss = { showEdit = false },
            onConfirm = { name, concept, experience, size, legs, manaEnabled, manaMaximum ->
                controller.updateActive {
                    it.copy(
                        name = name.ifBlank { "Новый персонаж" },
                        concept = concept,
                        experience = experience,
                        size = size,
                        legs = legs,
                        manaEnabled = manaEnabled,
                        manaMaximum = manaMaximum,
                    )
                }
                showEdit = false
            },
        )
    }
}

@Composable
private fun CharacterHeader(character: DublCharacter, onEdit: () -> Unit) {
    DublCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.headlineMedium)
                if (character.concept.isNotBlank()) {
                    Text(
                        character.concept,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Опыт ${character.experience}  •  Размер ${character.size}  •  ${if (character.legs >= 3) "3+ ног" else "2 ноги"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onEdit) { Text("Изменить") }
        }
    }
}

@Composable
private fun Resources(character: DublCharacter, controller: CharacterController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ResourceCard(
            title = "Здоровье",
            current = character.hpCurrent,
            maximum = character.healthMaximum,
            onMinus = { controller.changeHp(-1) },
            onPlus = { controller.changeHp(1) },
        )
        ResourceCard(
            title = "Выносливость",
            current = character.enduranceCurrent,
            maximum = 3,
            onMinus = { controller.changeEndurance(-1) },
            onPlus = { controller.changeEndurance(1) },
        )
        if (character.manaEnabled) {
            ResourceCard(
                title = "Мана",
                current = character.manaCurrent,
                maximum = character.manaMaximum,
                onMinus = { controller.changeMana(-1) },
                onPlus = { controller.changeMana(1) },
            )
        }
    }
}

@Composable
private fun ResourceCard(
    title: String,
    current: Int,
    maximum: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    DublCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$current / $maximum",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMinus) { Text("−") }
                Button(onClick = onPlus) { Text("+") }
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = {
                if (maximum <= 0) 0f else (current.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AttributeGrid(character: DublCharacter, controller: CharacterController) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 620.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AttributeId.entries.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { id ->
                        AttributeCard(
                            id = id,
                            character = character,
                            onMinus = { controller.changeAttribute(id, -1) },
                            onPlus = { controller.changeAttribute(id, 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AttributeCard(
    id: AttributeId,
    character: DublCharacter,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val raw = character.attributeRaw(id)
    val total = character.attribute(id)
    DublCard(modifier) {
        Text(id.shortTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(total.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(id.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (raw != total) {
            Text(
                "База $raw • размер ${if (total - raw >= 0) "+" else ""}${total - raw}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onMinus, modifier = Modifier.weight(1f)) { Text("−") }
            OutlinedButton(onClick = onPlus, modifier = Modifier.weight(1f)) { Text("+") }
        }
    }
}

@Composable
private fun EditCharacterDialog(
    character: DublCharacter,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int, Int, Boolean, Int) -> Unit,
) {
    var name by remember(character.id) { mutableStateOf(character.name) }
    var concept by remember(character.id) { mutableStateOf(character.concept) }
    var experience by remember(character.id) { mutableStateOf(character.experience.toString()) }
    var size by remember(character.id) { mutableStateOf(character.size.toString()) }
    var legs by remember(character.id) { mutableStateOf(character.legs.toString()) }
    var manaEnabled by remember(character.id) { mutableStateOf(character.manaEnabled) }
    var manaMaximum by remember(character.id) { mutableStateOf(character.manaMaximum.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Персонаж") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(concept, { concept = it }, label = { Text("Концепт") })
                NumericField("Опыт", experience) { experience = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField("Размер 1–10", size, Modifier.weight(1f)) { size = it }
                    NumericField("Количество ног", legs, Modifier.weight(1f)) { legs = it }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Использовать ману")
                    Switch(checked = manaEnabled, onCheckedChange = { manaEnabled = it })
                }
                if (manaEnabled) {
                    NumericField("Максимум маны", manaMaximum) { manaMaximum = it }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        concept,
                        experience.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        size.toIntOrNull()?.coerceIn(1, 10) ?: 5,
                        legs.toIntOrNull()?.coerceAtLeast(2) ?: 2,
                        manaEnabled,
                        manaMaximum.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    )
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '-' }) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

private fun formatNumber(value: Double): String = DecimalFormat("0.##").format(value)
