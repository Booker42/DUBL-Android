package com.dubl.character.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.theme.DublAccent
import com.dubl.character.android.ui.theme.DublAccentSoft
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublHealth
import com.dubl.character.android.ui.theme.DublMana
import com.dubl.character.android.ui.theme.DublStamina
import java.text.DecimalFormat

enum class CharacterResource {
    HEALTH,
    ENDURANCE,
    MANA,
}

@Composable
fun OverviewScreen(controller: CharacterController) {
    val character = controller.active
    var showProfileEdit by remember { mutableStateOf(false) }
    var selectedResource by remember { mutableStateOf<CharacterResource?>(null) }
    var selectedAttribute by remember { mutableStateOf<AttributeId?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            CharacterHero(
                character = character,
                onEdit = { showProfileEdit = true },
            )
        }

        item {
            SectionTitle("Ресурсы")
            Spacer(Modifier.height(8.dp))
            ResourceStrip(
                character = character,
                onResourceClick = { selectedResource = it },
            )
        }

        item {
            SectionTitle("Показатели")
            Spacer(Modifier.height(8.dp))
            KeyStats(character)
        }

        item {
            SectionTitle("Характеристики", trailing = "Нажмите, чтобы изменить")
            Spacer(Modifier.height(8.dp))
            AttributeGrid(
                character = character,
                onAttributeClick = { selectedAttribute = it },
            )
        }
    }

    if (showProfileEdit) {
        EditCharacterDialog(
            character = character,
            onDismiss = { showProfileEdit = false },
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
                showProfileEdit = false
            },
        )
    }

    selectedResource?.let { resource ->
        when (resource) {
            CharacterResource.HEALTH -> {
                HealthControlSheet(
                    current = character.hpCurrent,
                    maximum = character.healthMaximum,
                    onChange = controller::changeHp,
                    onDismiss = { selectedResource = null },
                )
            }
            CharacterResource.ENDURANCE -> {
                ResourceAdjustSheet(
                    title = "Выносливость",
                    current = character.enduranceCurrent,
                    maximum = 3,
                    accent = DublStamina,
                    onChange = controller::changeEndurance,
                    onDismiss = { selectedResource = null },
                )
            }
            CharacterResource.MANA -> {
                ResourceAdjustSheet(
                    title = "Мана",
                    current = character.manaCurrent,
                    maximum = character.manaMaximum,
                    accent = DublMana,
                    onChange = controller::changeMana,
                    onDismiss = { selectedResource = null },
                )
            }
        }
    }

    selectedAttribute?.let { id ->
        AttributeAdjustSheet(
            id = id,
            character = character,
            onMinus = { controller.changeAttribute(id, -1) },
            onPlus = { controller.changeAttribute(id, 1) },
            onDismiss = { selectedAttribute = null },
        )
    }

}

@Composable
private fun CharacterHero(character: DublCharacter, onEdit: () -> Unit) {
    DublCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(62.dp),
                shape = RoundedCornerShape(15.dp),
                color = DublAccentSoft,
                border = BorderStroke(1.dp, DublAccent.copy(alpha = 0.62f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = character.name.trim().firstOrNull()?.uppercase() ?: "D",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = character.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    TextButton(onClick = onEdit) {
                        Text("Изменить")
                    }
                }

                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    MetaPill(
                        label = "Опыт",
                        value = character.experience.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    MetaPill(
                        label = "Очки способностей",
                        value = character.abilityPoints.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DublGold.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, DublGold.copy(alpha = 0.46f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DublGold.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = value,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DublGold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(width = 4.dp, height = 18.dp),
                shape = RoundedCornerShape(2.dp),
                color = DublAccent,
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResourceStrip(
    character: DublCharacter,
    onResourceClick: (CharacterResource) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactResourceCard(
            title = "Здоровье",
            current = character.hpCurrent,
            maximum = character.healthMaximum,
            accent = DublHealth,
            modifier = Modifier.weight(1f),
            onClick = { onResourceClick(CharacterResource.HEALTH) },
        )
        CompactResourceCard(
            title = "Выносливость",
            current = character.enduranceCurrent,
            maximum = 3,
            accent = DublStamina,
            modifier = Modifier.weight(1f),
            onClick = { onResourceClick(CharacterResource.ENDURANCE) },
        )
        if (character.manaEnabled) {
            CompactResourceCard(
                title = "Мана",
                current = character.manaCurrent,
                maximum = character.manaMaximum,
                accent = DublMana,
                modifier = Modifier.weight(1f),
                onClick = { onResourceClick(CharacterResource.MANA) },
            )
        }
    }
}

@Composable
private fun CompactResourceCard(
    title: String,
    current: Int,
    maximum: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$current / $maximum",
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (maximum <= 0) 0f else (current.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
            )
        }
    }
}

@Composable
private fun KeyStats(character: DublCharacter) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("Защита", character.defense.toString(), Modifier.weight(1f), prominent = true)
            StatTile("Рефлексы", signed(character.reflexes), Modifier.weight(1f))
            StatTile("Инициатива", signed(character.initiative), Modifier.weight(1f), prominent = true)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("Стойкость", signed(character.fortitude), Modifier.weight(1f))
            StatTile("Бег", formatNumber(character.runFull), Modifier.weight(1f), suffix = " м")
            StatTile("Размер", character.size.toString(), Modifier.weight(1f))
        }
    }
}
@Composable
private fun StatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    prominent: Boolean = false,
    accent: Color? = null,
) {
    val valueAccent = accent ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (prominent) DublAccentSoft.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (prominent) DublAccent.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value + suffix,
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                color = if (prominent) MaterialTheme.colorScheme.onSurface else valueAccent,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AttributeGrid(
    character: DublCharacter,
    onAttributeClick: (AttributeId) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 620.dp) 4 else 2
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
                            modifier = Modifier.weight(1f),
                            onClick = { onAttributeClick(id) },
                        )
                    }
                    repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun attributeAccent(id: AttributeId): Color = when (id) {
    AttributeId.STRENGTH -> Color(0xFFB96363)
    AttributeId.DEXTERITY -> Color(0xFFC08D52)
    AttributeId.CONSTITUTION -> Color(0xFF7FA06F)
    AttributeId.SPEED -> Color(0xFF65A0A8)
    AttributeId.INTELLIGENCE -> Color(0xFF718FB8)
    AttributeId.PERCEPTION -> Color(0xFF8E80B5)
    AttributeId.WILL -> Color(0xFFA87193)
    AttributeId.CHARISMA -> Color(0xFFC07E6D)
}

@Composable
private fun AttributeCard(
    id: AttributeId,
    character: DublCharacter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val raw = character.attributeRaw(id)
    val total = character.attribute(id)
    val sizeDelta = total - raw
    val accent = attributeAccent(id)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.055f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.72f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = id.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (sizeDelta != 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "База $raw · размер ${signed(sizeDelta)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = DublGold,
                    )
                }
            }
            Text(
                text = total.toString(),
                fontSize = 31.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceAdjustSheet(
    title: String,
    current: Int,
    maximum: Int,
    accent: Color,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "$current / $maximum",
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onChange(-1) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("− 1") }
                Button(
                    onClick = { onChange(1) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("+ 1") }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Значение ограничено диапазоном 0–$maximum",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeAdjustSheet(
    id: AttributeId,
    character: DublCharacter,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val raw = character.attributeRaw(id)
    val total = character.attribute(id)
    val delta = total - raw

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(id.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                total.toString(),
                fontSize = 42.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            if (delta != 0) {
                Text(
                    "База $raw · модификатор размера ${signed(delta)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DublGold,
                )
            } else {
                Text(
                    "Базовое значение $raw",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onMinus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("− 1") }
                Button(
                    onClick = onPlus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("+ 1") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthControlSheet(
    current: Int,
    maximum: Int,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember(current, maximum) { mutableStateOf("1") }
    val amount = amountText.toIntOrNull()?.coerceAtLeast(0) ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Здоровье", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "$current / $maximum",
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
                color = DublHealth,
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Количество") },
                supportingText = { Text("Нажмите на число и введите урон или лечение") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (amount > 0) onChange(-amount)
                        onDismiss()
                    },
                    enabled = amount > 0 && current > 0,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DublAccent),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Получить урон")
                }
                OutlinedButton(
                    onClick = {
                        if (amount > 0) onChange(amount)
                        onDismiss()
                    },
                    enabled = amount > 0 && current < maximum,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Лечение")
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    onChange(maximum - current)
                    onDismiss()
                },
                enabled = current < maximum,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Восстановить всё здоровье")
            }
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
        title = { Text("Редактировать персонажа") },
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

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()

private fun formatNumber(value: Double): String = DecimalFormat("0.##").format(value)
