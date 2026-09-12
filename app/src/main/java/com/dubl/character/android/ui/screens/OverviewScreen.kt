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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.dubl.character.android.ui.theme.DublBorder
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublHealth
import com.dubl.character.android.ui.theme.DublMana
import com.dubl.character.android.ui.theme.DublStamina
import com.dubl.character.android.ui.theme.DublSurfaceInset
import java.text.DecimalFormat

enum class CharacterResource {
    HEALTH,
    ENDURANCE,
    MANA,
}

private enum class HealthAction(val title: String, val sign: Int) {
    DAMAGE("Получить урон", -1),
    HEAL("Восстановить здоровье", 1),
}

@Composable
fun OverviewScreen(controller: CharacterController) {
    val character = controller.active
    var showProfileEdit by remember { mutableStateOf(false) }
    var selectedResource by remember { mutableStateOf<CharacterResource?>(null) }
    var selectedAttribute by remember { mutableStateOf<AttributeId?>(null) }
    var healthAction by remember { mutableStateOf<HealthAction?>(null) }

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
            Spacer(Modifier.height(10.dp))
            QuickActions(
                onDamage = { healthAction = HealthAction.DAMAGE },
                onHeal = { healthAction = HealthAction.HEAL },
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
        val title: String
        val current: Int
        val maximum: Int
        val color: Color
        val change: (Int) -> Unit
        when (resource) {
            CharacterResource.HEALTH -> {
                title = "Здоровье"
                current = character.hpCurrent
                maximum = character.healthMaximum
                color = DublHealth
                change = controller::changeHp
            }
            CharacterResource.ENDURANCE -> {
                title = "Выносливость"
                current = character.enduranceCurrent
                maximum = 3
                color = DublStamina
                change = controller::changeEndurance
            }
            CharacterResource.MANA -> {
                title = "Мана"
                current = character.manaCurrent
                maximum = character.manaMaximum
                color = DublMana
                change = controller::changeMana
            }
        }
        ResourceAdjustSheet(
            title = title,
            current = current,
            maximum = maximum,
            accent = color,
            onChange = change,
            onDismiss = { selectedResource = null },
        )
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

    healthAction?.let { action ->
        HealthActionSheet(
            action = action,
            current = character.hpCurrent,
            maximum = character.healthMaximum,
            onApply = { amount -> controller.changeHp(action.sign * amount) },
            onDismiss = { healthAction = null },
        )
    }
}

@Composable
private fun CharacterHero(character: DublCharacter, onEdit: () -> Unit) {
    DublCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(70.dp),
                shape = RoundedCornerShape(16.dp),
                color = DublAccentSoft,
                border = BorderStroke(1.dp, DublAccent.copy(alpha = 0.65f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = character.name.trim().firstOrNull()?.uppercase() ?: "D",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                )
                if (character.concept.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = character.concept,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaPill("Опыт ${character.experience}")
                    MetaPill("Размер ${character.size}")
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (character.legs >= 3) "${character.legs} ног" else "2 ноги",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onEdit) {
                Text("Изменить персонажа")
            }
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = DublSurfaceInset,
        border = BorderStroke(1.dp, DublBorder),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
private fun QuickActions(onDamage: () -> Unit, onHeal: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onDamage,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DublAccent),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Получить урон")
        }
        OutlinedButton(
            onClick = onHeal,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Лечение")
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
            StatTile("Инициатива", signed(character.initiative), Modifier.weight(1f), prominent = true)
            StatTile("Бег", formatNumber(character.runFull), Modifier.weight(1f), suffix = " м")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("Рефлексы", signed(character.reflexes), Modifier.weight(1f))
            StatTile("Стойкость", signed(character.fortitude), Modifier.weight(1f))
            StatTile("Очки способн.", character.abilityPoints.toString(), Modifier.weight(1f), accent = DublGold)
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

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = id.shortTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = id.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (sizeDelta != 0) {
                    Text(
                        text = "База $raw · размер ${signed(sizeDelta)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = DublGold,
                    )
                }
            }
            Text(
                text = total.toString(),
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
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
private fun HealthActionSheet(
    action: HealthAction,
    current: Int,
    maximum: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember(action) { mutableIntStateOf(1) }
    val result = (current + action.sign * amount).coerceIn(0, maximum)

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
            Text(action.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                OutlinedButton(onClick = { amount = (amount - 1).coerceAtLeast(1) }) { Text("−") }
                Text(amount.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { amount += 1 }) { Text("+") }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "$current → $result здоровья",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    onApply(amount)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (action == HealthAction.DAMAGE) DublAccent else DublGold,
                    contentColor = if (action == HealthAction.DAMAGE) Color.White else Color(0xFF211B13),
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Применить")
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
