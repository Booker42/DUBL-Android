package com.dubl.character.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.ResolvedSkill
import com.dubl.character.android.model.SkillCatalog
import com.dubl.character.android.model.SkillCategory
import com.dubl.character.android.model.UntrainedRule
import com.dubl.character.android.model.resolveSkill
import com.dubl.character.android.model.resolvedSkills
import com.dubl.character.android.model.skillCalculation
import com.dubl.character.android.model.skillXpSpent
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard

@Composable
fun SkillsScreen(controller: CharacterController) {
    val character = controller.active
    var query by remember(character.id) { mutableStateOf("") }
    var selectedCategory by remember(character.id) { mutableStateOf<SkillCategory?>(null) }
    var trainedOnly by remember(character.id) { mutableStateOf(false) }
    var selectedSkillId by remember(character.id) { mutableStateOf<String?>(null) }
    var showAdd by remember(character.id) { mutableStateOf(false) }
    var showHidden by remember(character.id) { mutableStateOf(false) }

    val visibleSkills = character.resolvedSkills()
    val filtered = visibleSkills.filter { skill ->
        val needle = query.trim()
        (selectedCategory == null || skill.category == selectedCategory) &&
            (!trainedOnly || skill.rank > 0) &&
            (needle.isBlank() || skill.name.contains(needle, ignoreCase = true) ||
                skill.description.contains(needle, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Умения", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        character.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = { showAdd = true }) { Text("+ Умение") }
            }
        }

        item {
            DublCard(Modifier.fillMaxWidth()) {
                val trained = character.resolvedSkills(includeHidden = true).count { it.rank > 0 }
                Text(
                    "Изучено $trained • XP в умения ${character.skillXpSpent()}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Ранг 0–10 • итог = выбранные характеристики + ранг + поправка",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск по умениям") },
                singleLine = true,
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Все") },
                    )
                }
                items(SkillCategory.entries.filter { category ->
                    visibleSkills.any { it.category == category }
                }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text(category.title) },
                    )
                }
                item {
                    FilterChip(
                        selected = trainedOnly,
                        onClick = { trainedOnly = !trainedOnly },
                        label = { Text("Изученные") },
                    )
                }
            }
        }

        if (character.hiddenSkillIds.isNotEmpty()) {
            item {
                TextButton(onClick = { showHidden = true }) {
                    Text("Скрытые умения: ${character.hiddenSkillIds.size}")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                DublCard(Modifier.fillMaxWidth()) {
                    Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Сбросьте поиск/фильтр или верните скрытые умения.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            SkillCategory.entries.forEach { category ->
                val categorySkills = filtered.filter { it.category == category }
                if (categorySkills.isNotEmpty()) {
                    item(key = "header-${category.name}") {
                        Text(
                            category.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    items(categorySkills, key = { it.id }) { skill ->
                        SkillCard(
                            character = character,
                            skill = skill,
                            onClick = { selectedSkillId = skill.id },
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(18.dp)) }
    }

    val selected = selectedSkillId?.let { character.resolveSkill(it) }
    if (selected != null) {
        SkillDetailSheet(
            character = character,
            skill = selected,
            controller = controller,
            onDismiss = { selectedSkillId = null },
        )
    }

    if (showAdd) {
        AddSkillDialog(
            controller = controller,
            onDismiss = { showAdd = false },
            onAdded = { id ->
                showAdd = false
                selectedSkillId = id
            },
        )
    }

    if (showHidden) {
        HiddenSkillsDialog(
            character = character,
            controller = controller,
            onDismiss = { showHidden = false },
        )
    }
}

@Composable
private fun SkillCard(
    character: DublCharacter,
    skill: ResolvedSkill,
    onClick: () -> Unit,
) {
    val calculation = character.skillCalculation(skill)
    DublCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    skill.attributes.joinToString(" + ") { it.shortTitle },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("Ранг ${skill.rank}", style = MaterialTheme.typography.labelLarge)
                Text(
                    calculation.total?.let { if (it >= 0) "+$it" else it.toString() } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        if (skill.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                skill.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        if (calculation.unavailableReason.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(calculation.unavailableReason, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillDetailSheet(
    character: DublCharacter,
    skill: ResolvedSkill,
    controller: CharacterController,
    onDismiss: () -> Unit,
) {
    var note by remember(skill.id, skill.formulaNote) { mutableStateOf(skill.formulaNote) }
    val calculation = character.skillCalculation(skill)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(skill.name, style = MaterialTheme.typography.headlineSmall)
            if (skill.description.isNotBlank()) {
                Text(skill.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            DublCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Ранг", style = MaterialTheme.typography.labelLarge)
                        Text(skill.rank.toString(), style = MaterialTheme.typography.headlineMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { controller.changeSkillRank(skill.id, -1) },
                            enabled = skill.rank > 0,
                        ) { Text("−") }
                        Button(
                            onClick = { controller.changeSkillRank(skill.id, 1) },
                            enabled = skill.rank < 10,
                        ) { Text("+") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Потрачено XP: ${SkillCatalog.costForRank(skill.rank)}" +
                        (SkillCatalog.nextRankCost(skill.rank)?.let { " • следующий ранг +$it XP" } ?: " • максимум"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Характеристики", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AttributeId.entries) { attribute ->
                    val selected = attribute in skill.attributes
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val updated = if (selected) {
                                if (skill.attributes.size == 1) skill.attributes else skill.attributes - attribute
                            } else {
                                skill.attributes + attribute
                            }
                            controller.setSkillAttributes(skill.id, updated)
                        },
                        label = { Text(attribute.shortTitle) },
                    )
                }
            }

            DublCard(Modifier.fillMaxWidth()) {
                Text("Итог", style = MaterialTheme.typography.labelLarge)
                Text(
                    calculation.total?.let { if (it >= 0) "+$it" else it.toString() } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    calculation.formulaText(skill),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Поправка", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { controller.setSkillModifier(skill.id, skill.modifier - 1) }) { Text("−") }
                Text(
                    if (skill.modifier >= 0) "+${skill.modifier}" else skill.modifier.toString(),
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedButton(onClick = { controller.setSkillModifier(skill.id, skill.modifier + 1) }) { Text("+") }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Примечание к расчёту") },
                minLines = 2,
            )
            Button(
                onClick = { controller.setSkillFormulaNote(skill.id, note) },
                enabled = note.trim() != skill.formulaNote,
            ) { Text("Сохранить примечание") }

            HorizontalDivider()
            Text("Без обучения: ${skill.untrained.label}")
            skill.definition?.let { definition ->
                Text("Автоуспех 6: ${definition.auto6} • Автоуспех 12: ${definition.auto12}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        controller.hideSkill(skill.id)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Скрыть") }
                if (skill.isDynamic) {
                    TextButton(
                        onClick = {
                            controller.deleteDynamicSkill(skill.id)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Удалить") }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AddSkillDialog(
    controller: CharacterController,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit,
) {
    var customMode by remember { mutableStateOf(false) }
    var templateId by remember { mutableStateOf(SkillCatalog.templates.first().id) }
    var specialization by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }
    var customAttrs by remember { mutableStateOf(listOf(AttributeId.INTELLIGENCE)) }
    var customUntrained by remember { mutableStateOf(UntrainedRule.YES) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить умение") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !customMode,
                        onClick = { customMode = false; error = "" },
                        label = { Text("Специализация") },
                    )
                    FilterChip(
                        selected = customMode,
                        onClick = { customMode = true; error = "" },
                        label = { Text("Своё") },
                    )
                }

                if (!customMode) {
                    Text("Тип", style = MaterialTheme.typography.titleMedium)
                    SkillCatalog.templates.forEach { template ->
                        FilterChip(
                            selected = templateId == template.id,
                            onClick = { templateId = template.id },
                            label = { Text(template.name) },
                        )
                    }
                    OutlinedTextField(
                        value = specialization,
                        onValueChange = { specialization = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Специализация") },
                        placeholder = { Text("Например: Биология") },
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Название") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customDescription,
                        onValueChange = { customDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Описание") },
                        minLines = 2,
                    )
                    Text("Характеристики", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AttributeId.entries) { attribute ->
                            val selected = attribute in customAttrs
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    customAttrs = if (selected) {
                                        if (customAttrs.size == 1) customAttrs else customAttrs - attribute
                                    } else customAttrs + attribute
                                },
                                label = { Text(attribute.shortTitle) },
                            )
                        }
                    }
                    Text("Без обучения", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(UntrainedRule.YES, UntrainedRule.YES_MINUS_2, UntrainedRule.NO).forEach { rule ->
                            FilterChip(
                                selected = customUntrained == rule,
                                onClick = { customUntrained = rule },
                                label = { Text(rule.label) },
                            )
                        }
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = if (customMode) {
                        controller.addCustomSkill(customName, customDescription, customAttrs, customUntrained)
                    } else {
                        controller.addSpecializedSkill(templateId, specialization)
                    }
                    if (id == null) {
                        error = "Введите корректное уникальное название. Такое умение уже может существовать."
                    } else {
                        onAdded(id)
                    }
                },
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun HiddenSkillsDialog(
    character: DublCharacter,
    controller: CharacterController,
    onDismiss: () -> Unit,
) {
    val hidden = character.resolvedSkills(includeHidden = true).filter { it.id in character.hiddenSkillIds }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Скрытые умения") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                hidden.forEach { skill ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(skill.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { controller.restoreSkill(skill.id) }) { Text("Вернуть") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    controller.restoreAllSkills()
                    onDismiss()
                },
            ) { Text("Вернуть все") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}
