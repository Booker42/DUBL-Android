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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubl.character.android.data.DevelopmentCatalogRepository
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DevelopmentCostType
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.DevelopmentProgress
import com.dubl.character.android.model.DevelopmentRules
import com.dubl.character.android.model.MagicEquipmentRules
import com.dubl.character.android.model.RequirementCheck
import com.dubl.character.android.model.RequirementStatus
import com.dubl.character.android.model.developmentNormalize
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.components.DublScreenHeader
import com.dubl.character.android.ui.theme.DublAccent
import com.dubl.character.android.ui.theme.DublDanger
import com.dubl.character.android.ui.theme.DublGold

private enum class DevelopmentFilter(val title: String) {
    ALL("Все"),
    FEATS("Навыки"),
    ABILITIES("Способности"),
}

private data class PendingAbilityPurchase(
    val entry: DevelopmentEntry,
    val optionIndex: Int,
)

@Composable
fun FeatsScreen(controller: CharacterController) {
    val character = controller.active
    val context = LocalContext.current
    val catalog = remember(context.applicationContext) {
        DevelopmentCatalogRepository(context.applicationContext).load()
    }
    val progress = DevelopmentProgress(character.development)
    var query by remember(character.id) { mutableStateOf("") }
    var typeFilter by remember(character.id) { mutableStateOf(DevelopmentFilter.ALL) }
    var availableOnly by remember(character.id) { mutableStateOf(false) }
    var ownedOnly by remember(character.id) { mutableStateOf(false) }
    var selectedEntryId by remember(character.id) { mutableStateOf<String?>(null) }
    var pendingAbilityPurchase by remember(character.id) { mutableStateOf<PendingAbilityPurchase?>(null) }

    val rules = remember(character, progress, catalog) {
        DevelopmentRules(character, catalog, progress)
    }

    fun increase(entry: DevelopmentEntry, optionIndex: Int) {
        val availability = rules.availability(entry, optionIndex)
        if (!availability.canIncrease) return
        if (entry.isAbility) {
            pendingAbilityPurchase = PendingAbilityPurchase(entry, optionIndex)
        } else {
            controller.setDevelopmentRank(entry.id, availability.currentRank + 1, optionIndex)
        }
    }

    fun decrease(entry: DevelopmentEntry) {
        val current = progress.rank(entry.id)
        if (current <= 0) return
        controller.setDevelopmentRank(entry.id, current - 1, progress.optionIndex(entry.id))
    }

    val filteredEntries = remember(query, typeFilter, availableOnly, ownedOnly, character, progress, catalog) {
        val localRules = DevelopmentRules(character, catalog, progress)
        val needle = developmentNormalize(query)
        catalog.entries
            .asSequence()
            .filterNot { it.incomplete }
            .filterNot { it.id == MagicEquipmentRules.BASE_MANA_ENTRY_ID }
            .filter { entry ->
                when (typeFilter) {
                    DevelopmentFilter.ALL -> true
                    DevelopmentFilter.FEATS -> entry.costType == DevelopmentCostType.XP
                    DevelopmentFilter.ABILITIES -> entry.costType == DevelopmentCostType.ABILITY
                }
            }
            .filter { entry ->
                if (needle.isBlank()) true else developmentNormalize(
                    listOf(
                        entry.name,
                        entry.category,
                        entry.section,
                        entry.requirements,
                        entry.benefit,
                        entry.notes,
                        entry.tags.joinToString(" "),
                    ).joinToString(" ")
                ).contains(needle)
            }
            .filter { entry -> !ownedOnly || progress.rank(entry.id) > 0 }
            .filter { entry -> !availableOnly || localRules.availability(entry).canIncrease }
            .sortedWith(compareBy<DevelopmentEntry>({ developmentNormalize(it.category) }, { developmentNormalize(it.name) }))
            .toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            DublScreenHeader(
                title = "Навыки",
                subtitle = "Навыки, способности и требования",
            )
        }

        item {
            DevelopmentBudgetCard(rules)
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск навыка или способности") },
                singleLine = true,
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(DevelopmentFilter.entries) { filter ->
                    FilterChip(
                        selected = typeFilter == filter,
                        onClick = { typeFilter = filter },
                        label = { Text(filter.title) },
                    )
                }
                item {
                    FilterChip(
                        selected = availableOnly,
                        onClick = {
                            availableOnly = !availableOnly
                            if (availableOnly) ownedOnly = false
                        },
                        label = { Text("Доступно") },
                    )
                }
                item {
                    FilterChip(
                        selected = ownedOnly,
                        onClick = {
                            ownedOnly = !ownedOnly
                            if (ownedOnly) availableOnly = false
                        },
                        label = { Text("Получено") },
                    )
                }
            }
        }

        item {
            Text(
                "Записей: ${filteredEntries.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (filteredEntries.isEmpty()) {
            item {
                DublCard(Modifier.fillMaxWidth()) {
                    Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Сбросьте поиск или фильтры.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val grouped = filteredEntries.groupBy { it.category }
            grouped.forEach { (category, entries) ->
                item(key = "dev-header-$category") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 11.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            category.ifBlank { "Общие" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            entries.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        )
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    DevelopmentRow(
                        entry = entry,
                        rules = rules,
                        progress = progress,
                        onClick = { selectedEntryId = entry.id },
                    )
                }
            }
        }
    }

    selectedEntryId?.let { id ->
        catalog.byId(id)?.let { entry ->
            DevelopmentDetailSheet(
                entry = entry,
                catalog = catalog,
                progress = progress,
                rules = rules,
                onOpenEntry = { targetId -> selectedEntryId = targetId },
                onIncrease = { optionIndex -> increase(entry, optionIndex) },
                onDecrease = { decrease(entry) },
                onDismiss = { selectedEntryId = null },
            )
        } ?: run { selectedEntryId = null }
    }

    pendingAbilityPurchase?.let { pending ->
        val cost = rules.abilityCost(pending.entry, pending.optionIndex)
        AlertDialog(
            onDismissRequest = { pendingAbilityPurchase = null },
            title = { Text("Получить способность?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(pending.entry.name, fontWeight = FontWeight.Bold)
                    pending.entry.abilityOptions.getOrNull(pending.optionIndex)?.let { option ->
                        Text("Источник: ${option.source}")
                    }
                    Text("Будет потрачено $cost ОС.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = progress.rank(pending.entry.id)
                        controller.setDevelopmentRank(pending.entry.id, current + 1, pending.optionIndex)
                        pendingAbilityPurchase = null
                    },
                ) { Text("Получить · $cost ОС") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAbilityPurchase = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun DevelopmentBudgetCard(rules: DevelopmentRules) {
    val available = rules.abilityPointsAvailable()
    val budget = rules.abilityPointsBudget()
    val spent = rules.abilityPointsSpent()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DublGold.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, DublGold.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Очки способностей",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$available доступно",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (available > 0) DublGold else MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$spent / $budget ОС",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${rules.xpSpentOnDevelopment()} XP в навыках",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DevelopmentRow(
    entry: DevelopmentEntry,
    rules: DevelopmentRules,
    progress: DevelopmentProgress,
    onClick: () -> Unit,
) {
    val availability = rules.availability(entry)
    val rank = progress.rank(entry.id)
    val owned = rank > 0
    val invalidOwned = owned && availability.checks.any { it.status != RequirementStatus.OK }
    val accent = when {
        invalidOwned -> DublDanger
        owned -> DublGold
        availability.canIncrease -> DublAccent
        else -> MaterialTheme.colorScheme.outline
    }
    val status = when {
        invalidOwned -> "Проверить"
        owned -> "✓ $rank/${entry.maxRank}"
        availability.canIncrease -> "Доступно"
        availability.checks.any { it.status == RequirementStatus.MANUAL } -> "Проверить"
        entry.isAbility && availability.reason.contains("очков") -> "Нет ОС"
        else -> "Закрыто"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (owned || availability.canIncrease) accent.copy(alpha = 0.035f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = if (owned || availability.canIncrease) 0.42f else 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = if (owned || availability.canIncrease) 0.9f else 0.34f),
            ) {}
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (owned || availability.canIncrease) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(if (entry.isAbility) "Способность" else "Навык")
                        append(" · ")
                        if (entry.isAbility) {
                            if (entry.abilityOptions.isNotEmpty()) {
                                val values = entry.abilityOptions.map { it.value }.distinct().sorted()
                                append(values.joinToString("–"))
                                append(" ОС")
                            } else {
                                append(entry.cost).append(" ОС")
                            }
                        } else {
                            append(entry.cost).append(" опыта")
                        }
                        if (entry.maxRank > 1) append(" · до ${entry.maxRank} ранга")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (entry.accessId != null) {
                    Text(
                        "Ветка: ${entry.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            DevelopmentStatusPill(status = status, accent = accent)
        }
    }
}


@Composable
private fun DevelopmentStatusPill(
    status: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevelopmentDetailSheet(
    entry: DevelopmentEntry,
    catalog: DevelopmentCatalog,
    progress: DevelopmentProgress,
    rules: DevelopmentRules,
    onOpenEntry: (String) -> Unit,
    onIncrease: (Int) -> Unit,
    onDecrease: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currentRank = progress.rank(entry.id)
    var optionIndex by remember(entry.id, currentRank) {
        mutableStateOf(
            if (currentRank > 0) progress.optionIndex(entry.id)
            else 0
        )
    }
    val availability = rules.availability(entry, optionIndex)
    val children = catalog.childrenOf(entry.id)
    val ownedInvalid = currentRank > 0 && availability.checks.any { it.status != RequirementStatus.OK }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 690.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${entry.section} · ${entry.category}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (currentRank > 0) {
                    Text(
                        "Ранг $currentRank/${entry.maxRank}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (ownedInvalid) DublDanger else DublGold,
                    )
                }
            }

            if (entry.tags.isNotEmpty()) {
                Text(
                    entry.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = DublGold,
                )
            }

            if (entry.benefit.isNotBlank()) {
                DetailBlock("Эффект", entry.benefit)
            }
            if (entry.notes.isNotBlank()) {
                DetailBlock("Особое", entry.notes)
            }
            if (entry.conflictNote.isNotBlank()) {
                DetailBlock("Расхождение книги", entry.conflictNote)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Text("Требования", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            availability.checks.forEach { check ->
                RequirementRow(check = check, onOpenEntry = onOpenEntry)
            }

            if (ownedInvalid) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = DublDanger.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DublDanger.copy(alpha = 0.35f)),
                ) {
                    Text(
                        "Навык уже получен, но его текущие требования больше не выполнены. Он не удаляется автоматически.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = DublDanger,
                    )
                }
            }

            if (children.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text(
                    "Открывает ${children.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                children.take(8).forEach { child ->
                    TextButton(onClick = { onOpenEntry(child.id) }) {
                        Text(child.name, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (children.size > 8) {
                    Text(
                        "И ещё ${children.size - 8} записей в ветке",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            if (entry.isAbility && entry.abilityOptions.isNotEmpty()) {
                Text("Источник способности", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(entry.abilityOptions.indices.toList()) { index ->
                        val option = entry.abilityOptions[index]
                        FilterChip(
                            selected = optionIndex == index,
                            enabled = currentRank == 0,
                            onClick = { optionIndex = index },
                            label = { Text("${option.source} · ${option.value} ОС") },
                        )
                    }
                }
            }

            Text(
                if (entry.isAbility) {
                    val cost = rules.abilityCost(entry, optionIndex)
                    "Стоимость: $cost ОС · доступно ${rules.abilityPointsAvailable()} ОС"
                } else {
                    "Стоимость следующего ранга: ${entry.cost} опыта"
                },
                style = MaterialTheme.typography.labelLarge,
                color = DublGold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (currentRank > 0) {
                    OutlinedButton(
                        onClick = onDecrease,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (currentRank == 1) "Убрать" else "− ранг")
                    }
                }
                Button(
                    onClick = { onIncrease(optionIndex) },
                    enabled = availability.canIncrease,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            availability.canIncrease && entry.isAbility -> "Получить · ${availability.abilityCost} ОС"
                            availability.canIncrease && currentRank > 0 -> "+ ранг · ${entry.cost} XP"
                            availability.canIncrease -> "Получить · ${entry.cost} XP"
                            else -> availability.reason
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RequirementRow(
    check: RequirementCheck,
    onOpenEntry: (String) -> Unit,
) {
    val color = when (check.status) {
        RequirementStatus.OK -> Color(0xFF71A492)
        RequirementStatus.FAIL -> DublDanger
        RequirementStatus.MANUAL -> DublGold
    }
    val prefix = when (check.status) {
        RequirementStatus.OK -> "✓"
        RequirementStatus.FAIL -> "✕"
        RequirementStatus.MANUAL -> "?"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = check.targetEntryId != null) {
                check.targetEntryId?.let(onOpenEntry)
            },
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(prefix, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                check.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            if (check.targetEntryId != null) {
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
