package com.dubl.character.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dubl.character.android.data.MagicEquipmentCatalogRepository
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.MagicEquipmentRules
import com.dubl.character.android.model.MagicSchool
import com.dubl.character.android.model.SpellCatalogEntry
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.components.DublScreenHeader
import com.dubl.character.android.ui.theme.DublAccentSoft
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublMana
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.android.ui.theme.DublSurfaceRaised
import java.util.UUID

@Composable
fun MagicScreen(controller: CharacterController) {
    val character = controller.active
    val context = LocalContext.current.applicationContext
    val catalog = remember(context) { MagicEquipmentCatalogRepository(context).load() }
    var spellQuery by remember(character.id) { mutableStateOf("") }
    var selectedSpellUid by remember(character.id) { mutableStateOf<String?>(null) }
    var showCatalog by remember(character.id) { mutableStateOf(false) }
    var editSpell by remember(character.id) { mutableStateOf<KnownSpell?>(null) }
    var createSpell by remember(character.id) { mutableStateOf(false) }
    var editSchoolIndex by remember(character.id) { mutableStateOf<Int?>(null) }
    var createSchool by remember(character.id) { mutableStateOf(false) }

    val maxMana = character.effectiveManaMaximum
    val recovery = MagicEquipmentRules.manaRecoveryPerRound(character)
    val filteredSpells = remember(character.magic.spells, spellQuery) {
        val needle = spellQuery.trim().lowercase()
        character.magic.spells.filter { spell ->
            needle.isBlank() || listOf(spell.name, spell.school, spell.description, spell.action)
                .joinToString(" ")
                .lowercase()
                .contains(needle)
        }.sortedBy { it.name.lowercase() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DublScreenHeader(
                title = "Магия",
                subtitle = "Сила, школы и книга заклинаний",
            )
        }

        item {
            MagicCoreCard(
                manaRank = character.magic.manaRank,
                power = character.magic.power,
                currentMana = character.manaCurrent,
                maxMana = maxMana,
                recovery = recovery,
                spellXp = MagicEquipmentRules.learnedSpellXp(character),
                manaXp = MagicEquipmentRules.manaRankXp(character),
                onManaRank = controller::setMagicManaRank,
                onPower = controller::setMagicPower,
                onManaDelta = controller::changeMana,
            )
        }

        item {
            DublCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Школы магии", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (character.magic.schools.isEmpty()) "Школы ещё не добавлены" else "${character.magic.schools.size} записей",
                            style = MaterialTheme.typography.bodySmall,
                            color = DublMuted,
                        )
                    }
                    TextButton(onClick = { createSchool = true }) { Text("+ Добавить") }
                }
                if (character.magic.schools.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    character.magic.schools.forEachIndexed { index, school ->
                        SchoolRow(school = school, onClick = { editSchoolIndex = index })
                        if (index != character.magic.schools.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("Книга заклинаний", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${character.magic.spells.size} заклинаний", style = MaterialTheme.typography.bodySmall, color = DublMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { createSpell = true }) { Text("Своё") }
                    Button(onClick = { showCatalog = true }) { Text("Из книги") }
                }
            }
        }

        item {
            OutlinedTextField(
                value = spellQuery,
                onValueChange = { spellQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск по книге заклинаний") },
                singleLine = true,
            )
        }

        if (filteredSpells.isEmpty()) {
            item {
                DublCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (character.magic.spells.isEmpty()) "Книга заклинаний пуста" else "Ничего не найдено",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (character.magic.spells.isEmpty()) "Добавьте заклинание из каталога или создайте своё." else "Измените поисковый запрос.",
                        color = DublMuted,
                    )
                }
            }
        } else {
            items(filteredSpells, key = { it.uid }) { spell ->
                SpellRow(spell = spell, onClick = { selectedSpellUid = spell.uid })
            }
        }
    }

    if (showCatalog) {
        SpellCatalogSheet(
            entries = catalog.spells,
            ownedIds = character.magic.spells.mapNotNull { it.catalogId }.toSet(),
            onAdd = { controller.addCatalogSpell(it) },
            onDismiss = { showCatalog = false },
        )
    }

    selectedSpellUid?.let { uid ->
        character.magic.spells.firstOrNull { it.uid == uid }?.let { spell ->
            SpellDetailSheet(
                spell = spell,
                onEdit = { editSpell = spell; selectedSpellUid = null },
                onRemove = { controller.removeSpell(uid); selectedSpellUid = null },
                onToggleLearned = { learned -> controller.updateSpell(uid) { it.copy(learned = learned) } },
                onDismiss = { selectedSpellUid = null },
            )
        } ?: run { selectedSpellUid = null }
    }

    if (createSpell) {
        SpellEditDialog(
            initial = KnownSpell(uid = UUID.randomUUID().toString(), custom = true),
            title = "Своё заклинание",
            onSave = { controller.addCustomSpell(it); createSpell = false },
            onDismiss = { createSpell = false },
        )
    }

    editSpell?.let { spell ->
        SpellEditDialog(
            initial = spell,
            title = spell.name,
            onSave = { updated -> controller.updateSpell(spell.uid) { updated }; editSpell = null },
            onDismiss = { editSpell = null },
        )
    }

    if (createSchool) {
        SchoolEditDialog(
            initial = MagicSchool(),
            title = "Новая школа",
            onSave = { school ->
                controller.addMagicSchool(school.name, school.rank, school.note)
                createSchool = false
            },
            onDismiss = { createSchool = false },
        )
    }

    editSchoolIndex?.let { index ->
        character.magic.schools.getOrNull(index)?.let { school ->
            SchoolEditDialog(
                initial = school,
                title = school.name,
                showDelete = true,
                onSave = { updated ->
                    controller.updateMagicSchool(index, updated.name, updated.rank, updated.note)
                    editSchoolIndex = null
                },
                onDelete = { controller.removeMagicSchool(index); editSchoolIndex = null },
                onDismiss = { editSchoolIndex = null },
            )
        } ?: run { editSchoolIndex = null }
    }
}

@Composable
private fun MagicCoreCard(
    manaRank: Int,
    power: Int,
    currentMana: Int,
    maxMana: Int,
    recovery: Int,
    spellXp: Int,
    manaXp: Int,
    onManaRank: (Int) -> Unit,
    onPower: (Int) -> Unit,
    onManaDelta: (Int) -> Unit,
) {
    DublCard(Modifier.fillMaxWidth()) {
        Text("Магический потенциал", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MagicCounter(
                title = "Ранг запаса",
                value = manaRank,
                modifier = Modifier.weight(1f),
                onMinus = { onManaRank((manaRank - 1).coerceAtLeast(0)) },
                onPlus = { onManaRank((manaRank + 1).coerceAtMost(5)) },
            )
            MagicCounter(
                title = "Сила магии",
                value = power,
                modifier = Modifier.weight(1f),
                onMinus = { onPower((power - 1).coerceAtLeast(0)) },
                onPlus = { onPower(power + 1) },
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DublMana.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, DublMana.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Мана", style = MaterialTheme.typography.labelLarge, color = DublMuted)
                        Text("$currentMana / $maxMana", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DublMana)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onManaDelta(-1) }, enabled = currentMana > 0) { Text("−") }
                        TextButton(onClick = { onManaDelta(1) }, enabled = currentMana < maxMana) { Text("+") }
                    }
                }
                Text("Восстановление: +$recovery / раунд", style = MaterialTheme.typography.bodySmall, color = DublMuted)
                Text("Опыт: запас $manaXp · изученные заклинания $spellXp", style = MaterialTheme.typography.bodySmall, color = DublMuted)
            }
        }
    }
}

@Composable
private fun MagicCounter(
    title: String,
    value: Int,
    modifier: Modifier = Modifier,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = DublSurfaceRaised,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = DublMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMinus) { Text("−") }
                Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onPlus) { Text("+") }
            }
        }
    }
}

@Composable
private fun SchoolRow(school: MagicSchool, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(school.name, fontWeight = FontWeight.SemiBold)
            if (school.note.isNotBlank()) Text(school.note, style = MaterialTheme.typography.bodySmall, color = DublMuted, maxLines = 1)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = DublAccentSoft) {
            Text("ур. ${school.rank}", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = DublFocus)
        }
    }
}

@Composable
private fun SpellRow(spell: KnownSpell, onClick: () -> Unit) {
    DublCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(spell.name, fontWeight = FontWeight.Bold)
                    if (spell.custom) {
                        Spacer(Modifier.width(6.dp))
                        Text("СВОЁ", style = MaterialTheme.typography.labelSmall, color = DublGold)
                    }
                }
                Text(
                    listOfNotNull(
                        spell.school.takeIf { it.isNotBlank() },
                        spell.time.takeIf { it.isNotBlank() },
                        if (spell.learned) null else "не изучено",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = DublMuted,
                    maxLines = 1,
                )
            }
            Surface(shape = RoundedCornerShape(999.dp), color = DublMana.copy(alpha = 0.12f)) {
                Text("${spell.cost} маны", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = DublMana)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpellCatalogSheet(
    entries: List<SpellCatalogEntry>,
    ownedIds: Set<String>,
    onAdd: (SpellCatalogEntry) -> Boolean,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, entries) {
        val needle = query.trim().lowercase()
        entries.filter { entry ->
            needle.isBlank() || listOf(entry.name, entry.school, entry.description, entry.action)
                .joinToString(" ").lowercase().contains(needle)
        }.sortedBy { it.name.lowercase() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .padding(horizontal = 16.dp),
        ) {
            Text("Каталог заклинаний", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${entries.size} записей из desktop-каталога", color = DublMuted)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Поиск") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(filtered, key = { it.id }) { entry ->
                    val owned = entry.id in ownedIds
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, fontWeight = FontWeight.SemiBold)
                                Text("${entry.school.ifBlank { "Без школы" }} · ${entry.cost} маны", style = MaterialTheme.typography.bodySmall, color = DublMuted)
                            }
                            TextButton(onClick = { onAdd(entry) }, enabled = !owned && !entry.incomplete) {
                                Text(if (owned) "Есть" else if (entry.incomplete) "Черновик" else "+")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpellDetailSheet(
    spell: KnownSpell,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onToggleLearned: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(spell.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${spell.school.ifBlank { "Без школы" }} · ${spell.cost} маны", color = DublMana)
            DetailFact("Время", spell.time)
            DetailFact("Дальность", spell.range)
            DetailFact("Область", spell.area)
            DetailFact("Проверка", spell.action)
            DetailFact("Длительность", spell.duration)
            if (spell.description.isNotBlank()) {
                HorizontalDivider()
                Text("Описание", fontWeight = FontWeight.Bold)
                Text(spell.description)
            }
            if (spell.enhancement.isNotBlank()) {
                Text("Усиление", fontWeight = FontWeight.Bold, color = DublGold)
                Text(spell.enhancement)
            }
            if (spell.conflictNote.isNotBlank()) {
                Text(spell.conflictNote, style = MaterialTheme.typography.bodySmall, color = DublMuted)
            }
            val xp = spell.xpOverride ?: MagicEquipmentRules.learnXpCost(spell.cost)
            Text("Цена изучения: ${xp?.let { "$it опыта" } ?: "не определена"}", style = MaterialTheme.typography.bodySmall, color = DublMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Изучено", modifier = Modifier.weight(1f))
                Switch(checked = spell.learned, onCheckedChange = onToggleLearned)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) { Text("Удалить") }
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Изменить") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DetailFact(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(92.dp), style = MaterialTheme.typography.labelMedium, color = DublMuted)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SpellEditDialog(
    initial: KnownSpell,
    title: String,
    onSave: (KnownSpell) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial.uid) { mutableStateOf(initial.name.takeUnless { it == "Заклинание" } ?: "") }
    var school by remember(initial.uid) { mutableStateOf(initial.school) }
    var costText by remember(initial.uid) { mutableStateOf(initial.cost.toString()) }
    var time by remember(initial.uid) { mutableStateOf(initial.time) }
    var range by remember(initial.uid) { mutableStateOf(initial.range) }
    var area by remember(initial.uid) { mutableStateOf(initial.area) }
    var action by remember(initial.uid) { mutableStateOf(initial.action) }
    var duration by remember(initial.uid) { mutableStateOf(initial.duration) }
    var description by remember(initial.uid) { mutableStateOf(initial.description) }
    var enhancement by remember(initial.uid) { mutableStateOf(initial.enhancement) }
    var learned by remember(initial.uid) { mutableStateOf(initial.learned) }
    var manualXp by remember(initial.uid) { mutableStateOf(initial.xpOverride != null) }
    var xpText by remember(initial.uid) { mutableStateOf((initial.xpOverride ?: MagicEquipmentRules.learnXpCost(initial.cost) ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(school, { school = it }, label = { Text("Школа") }, singleLine = true)
                OutlinedTextField(costText, { costText = it.filter(Char::isDigit) }, label = { Text("Стоимость маны") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Время") }, singleLine = true)
                OutlinedTextField(range, { range = it }, label = { Text("Дальность") }, singleLine = true)
                OutlinedTextField(area, { area = it }, label = { Text("Область") }, singleLine = true)
                OutlinedTextField(action, { action = it }, label = { Text("Проверка") })
                OutlinedTextField(duration, { duration = it }, label = { Text("Длительность") })
                OutlinedTextField(description, { description = it }, label = { Text("Описание") }, minLines = 3)
                OutlinedTextField(enhancement, { enhancement = it }, label = { Text("Усиление") }, minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Изучено", modifier = Modifier.weight(1f))
                    Switch(learned, { learned = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Цена опыта вручную", modifier = Modifier.weight(1f))
                    Switch(manualXp, { manualXp = it })
                }
                if (manualXp) {
                    OutlinedTextField(xpText, { xpText = it.filter(Char::isDigit) }, label = { Text("Опыт за изучение") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isBlank()) return@Button
                    val cost = costText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    onSave(
                        initial.copy(
                            name = clean,
                            school = school.trim(),
                            cost = cost,
                            manaText = cost.toString(),
                            time = time.trim(),
                            range = range.trim(),
                            area = area.trim(),
                            action = action.trim(),
                            duration = duration.trim(),
                            description = description.trim(),
                            enhancement = enhancement.trim(),
                            learned = learned,
                            xpOverride = if (manualXp) xpText.toIntOrNull()?.coerceAtLeast(0) ?: 0 else null,
                        )
                    )
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun SchoolEditDialog(
    initial: MagicSchool,
    title: String,
    showDelete: Boolean = false,
    onSave: (MagicSchool) -> Unit,
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    var name by remember(initial.name) { mutableStateOf(initial.name.takeUnless { it == "Школа" } ?: "") }
    var rankText by remember(initial.name, initial.rank) { mutableStateOf(initial.rank.toString()) }
    var note by remember(initial.name) { mutableStateOf(initial.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(rankText, { rankText = it.filter(Char::isDigit) }, label = { Text("Уровень") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Заметки") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                onSave(MagicSchool(name.trim(), rankText.toIntOrNull()?.coerceAtLeast(0) ?: 0, note.trim()))
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Row {
                if (showDelete) TextButton(onClick = onDelete) { Text("Удалить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}
