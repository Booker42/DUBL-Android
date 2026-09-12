package com.dubl.character.android.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dubl.character.android.data.CharacterSheetExtrasRepository
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.QuickCheckId
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.theme.DublAccent
import com.dubl.character.android.ui.theme.DublAccentSoft
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublHealth
import com.dubl.character.android.ui.theme.DublMana
import com.dubl.character.android.ui.theme.DublStamina
import java.text.DecimalFormat
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class CharacterResource {
    HEALTH,
    ENDURANCE,
    MANA,
}

private data class RecentChange(
    val text: String,
    val accent: Color,
    val token: Long = System.nanoTime(),
)

@Composable
fun OverviewScreen(controller: CharacterController) {
    val character = controller.active
    val context = LocalContext.current
    val extrasRepository = remember(context.applicationContext) {
        CharacterSheetExtrasRepository(context.applicationContext)
    }
    var sheetExtras by remember(character.id) {
        mutableStateOf(extrasRepository.load(character.id))
    }
    val listState = rememberLazyListState()
    val compactHeroVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150
        }
    }

    var showProfileEdit by remember { mutableStateOf(false) }
    var showConditions by remember { mutableStateOf(false) }
    var showFavoritePicker by remember { mutableStateOf(false) }
    var selectedResource by remember { mutableStateOf<CharacterResource?>(null) }
    var selectedAttribute by remember { mutableStateOf<AttributeId?>(null) }
    var recentChange by remember(character.id) { mutableStateOf<RecentChange?>(null) }

    val portraitPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val updated = sheetExtras.copy(portraitUri = uri.toString())
            sheetExtras = updated
            extrasRepository.save(character.id, updated)
        }
    }

    LaunchedEffect(recentChange?.token) {
        if (recentChange != null) {
            delay(3200)
            recentChange = null
        }
    }

    fun recordResourceChange(label: String, delta: Int, accent: Color) {
        if (delta == 0) return
        val sign = if (delta > 0) "+" else "−"
        recentChange = RecentChange("$sign${abs(delta)} $label", accent)
    }

    val effectiveConditions = remember(sheetExtras.activeConditions, character.enduranceCurrent) {
        buildSet {
            addAll(sheetExtras.activeConditions)
            if (character.enduranceCurrent == 0) add(CharacterConditionId.WEAKNESS)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CharacterHero(
                    character = character,
                    portraitUri = sheetExtras.portraitUri,
                    onEdit = { showProfileEdit = true },
                    onPortraitClick = { portraitPicker.launch(arrayOf("image/*")) },
                )
            }

            item {
                SectionTitle("Ресурсы")
                Spacer(Modifier.height(8.dp))
                ResourceStrip(
                    character = character,
                    onResourceClick = { selectedResource = it },
                )
                Spacer(Modifier.height(8.dp))
                ConditionSummaryBar(
                    conditions = effectiveConditions,
                    autoWeakness = character.enduranceCurrent == 0,
                    onClick = { showConditions = true },
                )
                recentChange?.let {
                    Spacer(Modifier.height(8.dp))
                    RecentChangeBar(it)
                }
            }

            item {
                SectionTitle("Показатели")
                Spacer(Modifier.height(8.dp))
                KeyStats(character)
            }

            item {
                QuickChecksSection(
                    character = character,
                    favoriteChecks = sheetExtras.favoriteChecks,
                    onConfigure = { showFavoritePicker = true },
                )
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

        if (compactHeroVisible) {
            CompactHeroBar(
                character = character,
                portraitUri = sheetExtras.portraitUri,
                conditionCount = effectiveConditions.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .zIndex(2f),
                onHealthClick = { selectedResource = CharacterResource.HEALTH },
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

    if (showConditions) {
        ConditionPickerSheet(
            active = sheetExtras.activeConditions,
            autoWeakness = character.enduranceCurrent == 0,
            onToggle = { condition ->
                val next = sheetExtras.activeConditions.toMutableSet().apply {
                    if (!add(condition)) remove(condition)
                }
                val updated = sheetExtras.copy(activeConditions = next)
                sheetExtras = updated
                extrasRepository.save(character.id, updated)
            },
            onDismiss = { showConditions = false },
        )
    }

    if (showFavoritePicker) {
        FavoriteChecksSheet(
            selected = sheetExtras.favoriteChecks,
            onToggle = { check ->
                val current = sheetExtras.favoriteChecks
                val next = if (check in current) {
                    current - check
                } else if (current.size < 4) {
                    current + check
                } else {
                    current
                }
                if (next != current) {
                    val updated = sheetExtras.copy(favoriteChecks = next)
                    sheetExtras = updated
                    extrasRepository.save(character.id, updated)
                }
            },
            onDismiss = { showFavoritePicker = false },
        )
    }

    selectedResource?.let { resource ->
        when (resource) {
            CharacterResource.HEALTH -> {
                HealthControlSheet(
                    current = character.hpCurrent,
                    maximum = character.healthMaximum,
                    onChange = { requestedDelta ->
                        val before = character.hpCurrent
                        val after = (before + requestedDelta).coerceIn(0, character.healthMaximum)
                        controller.changeHp(requestedDelta)
                        recordResourceChange("здоровья", after - before, DublHealth)
                    },
                    onDismiss = { selectedResource = null },
                )
            }
            CharacterResource.ENDURANCE -> {
                ResourceAdjustSheet(
                    title = "Выносливость",
                    current = character.enduranceCurrent,
                    maximum = 3,
                    accent = DublStamina,
                    onChange = { requestedDelta ->
                        val before = character.enduranceCurrent
                        val after = (before + requestedDelta).coerceIn(0, 3)
                        controller.changeEndurance(requestedDelta)
                        recordResourceChange("выносливости", after - before, DublStamina)
                    },
                    onDismiss = { selectedResource = null },
                )
            }
            CharacterResource.MANA -> {
                ResourceAdjustSheet(
                    title = "Мана",
                    current = character.manaCurrent,
                    maximum = character.manaMaximum,
                    accent = DublMana,
                    onChange = { requestedDelta ->
                        val before = character.manaCurrent
                        val after = (before + requestedDelta).coerceIn(0, character.manaMaximum)
                        controller.changeMana(requestedDelta)
                        recordResourceChange("маны", after - before, DublMana)
                    },
                    onDismiss = { selectedResource = null },
                )
            }
        }
    }

    selectedAttribute?.let { id ->
        AttributeAdjustSheet(
            id = id,
            character = character,
            onMinus = {
                controller.changeAttribute(id, -1)
                recentChange = RecentChange("${id.title} −1", attributeAccent(id))
            },
            onPlus = {
                controller.changeAttribute(id, 1)
                recentChange = RecentChange("${id.title} +1", attributeAccent(id))
            },
            onDismiss = { selectedAttribute = null },
        )
    }
}

@Composable
private fun CharacterHero(
    character: DublCharacter,
    portraitUri: String?,
    onEdit: () -> Unit,
    onPortraitClick: () -> Unit,
) {
    DublCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CharacterPortrait(
                character = character,
                portraitUri = portraitUri,
                size = 68.dp,
                onClick = onPortraitClick,
                showHint = true,
            )

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
private fun CharacterPortrait(
    character: DublCharacter,
    portraitUri: String?,
    size: Dp,
    onClick: (() -> Unit)? = null,
    showHint: Boolean = false,
) {
    val bitmap = rememberPortraitBitmap(portraitUri)
    val shape = RoundedCornerShape(if (size >= 60.dp) 15.dp else 11.dp)
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Surface(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = DublAccentSoft,
        border = BorderStroke(1.dp, DublAccent.copy(alpha = 0.62f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Портрет ${character.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = character.name.trim().firstOrNull()?.uppercase() ?: "D",
                    fontSize = if (size >= 60.dp) 26.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (showHint) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                ) {
                    Text(
                        text = if (bitmap == null) "Фото" else "Сменить",
                        modifier = Modifier.padding(vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberPortraitBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

@Composable
private fun CompactHeroBar(
    character: DublCharacter,
    portraitUri: String?,
    conditionCount: Int,
    modifier: Modifier = Modifier,
    onHealthClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)),
        shadowElevation = 7.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CharacterPortrait(character = character, portraitUri = portraitUri, size = 38.dp)
            Text(
                text = character.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .clickable(onClick = onHealthClick),
                shape = RoundedCornerShape(9.dp),
                color = DublHealth.copy(alpha = 0.09f),
                border = BorderStroke(1.dp, DublHealth.copy(alpha = 0.42f)),
            ) {
                Text(
                    text = "HP ${character.hpCurrent}/${character.healthMaximum}",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = DublHealth,
                )
            }
            Text(
                text = "Вын ${character.enduranceCurrent}/3",
                style = MaterialTheme.typography.labelLarge,
                color = DublStamina,
            )
            if (conditionCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DublAccent.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = conditionCount.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = DublAccent,
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
private fun SectionTitle(
    title: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
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
            if (onTrailingClick != null) {
                TextButton(onClick = onTrailingClick) { Text(trailing) }
            } else {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    val animatedCurrent by animateIntAsState(
        targetValue = current,
        animationSpec = tween(durationMillis = 280),
        label = "$title value",
    )
    val targetProgress = if (maximum <= 0) 0f else (current.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 340),
        label = "$title progress",
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
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
                text = "$animatedCurrent / $maximum",
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
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
private fun ConditionSummaryBar(
    conditions: Set<CharacterConditionId>,
    autoWeakness: Boolean,
    onClick: () -> Unit,
) {
    val ordered = CharacterConditionId.entries.filter { it in conditions }
    val text = when {
        ordered.isEmpty() -> "Нет активных состояний"
        ordered.size <= 2 -> ordered.joinToString(" · ") { it.title }
        else -> ordered.take(2).joinToString(" · ") { it.title } + "  +${ordered.size - 2}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (ordered.isEmpty()) MaterialTheme.colorScheme.surface else DublAccentSoft.copy(alpha = 0.42f),
        border = BorderStroke(
            1.dp,
            if (ordered.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            else DublAccent.copy(alpha = 0.42f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Состояния",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (ordered.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else DublAccent,
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (autoWeakness) {
                Text(
                    text = "авто",
                    style = MaterialTheme.typography.labelSmall,
                    color = DublStamina,
                )
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentChangeBar(change: RecentChange) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = change.accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, change.accent.copy(alpha = 0.24f)),
    ) {
        Text(
            text = "Последнее изменение: ${change.text}",
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = change.accent,
        )
    }
}

@Composable
private fun KeyStats(character: DublCharacter) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("Защита", character.defense.toString(), statAccent(QuickCheckId.DEFENSE), Modifier.weight(1f))
            StatTile("Рефлексы", signed(character.reflexes), statAccent(QuickCheckId.REFLEXES), Modifier.weight(1f))
            StatTile("Инициатива", signed(character.initiative), statAccent(QuickCheckId.INITIATIVE), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile("Стойкость", signed(character.fortitude), statAccent(QuickCheckId.FORTITUDE), Modifier.weight(1f))
            StatTile("Бег", formatNumber(character.runFull), statAccent(QuickCheckId.RUN), Modifier.weight(1f), suffix = " м")
            StatTile("Размер", character.size.toString(), statAccent(QuickCheckId.SIZE), Modifier.weight(1f))
        }
    }
}

private fun statAccent(id: QuickCheckId): Color = when (id) {
    QuickCheckId.DEFENSE -> Color(0xFFBB5863)
    QuickCheckId.REFLEXES -> Color(0xFF65A0A8)
    QuickCheckId.INITIATIVE -> DublGold
    QuickCheckId.FORTITUDE -> Color(0xFF7FA06F)
    QuickCheckId.RUN -> Color(0xFF6C96B5)
    QuickCheckId.SIZE -> Color(0xFF8E80B5)
    QuickCheckId.STRENGTH -> attributeAccent(AttributeId.STRENGTH)
    QuickCheckId.DEXTERITY -> attributeAccent(AttributeId.DEXTERITY)
    QuickCheckId.CONSTITUTION -> attributeAccent(AttributeId.CONSTITUTION)
    QuickCheckId.SPEED -> attributeAccent(AttributeId.SPEED)
    QuickCheckId.INTELLIGENCE -> attributeAccent(AttributeId.INTELLIGENCE)
    QuickCheckId.PERCEPTION -> attributeAccent(AttributeId.PERCEPTION)
    QuickCheckId.WILL -> attributeAccent(AttributeId.WILL)
    QuickCheckId.CHARISMA -> attributeAccent(AttributeId.CHARISMA)
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.055f),
        border = BorderStroke(1.25.dp, accent.copy(alpha = 0.48f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(width = 26.dp, height = 3.dp),
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.9f),
            ) {}
            Spacer(Modifier.height(6.dp))
            Text(
                text = value + suffix,
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
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
private fun QuickChecksSection(
    character: DublCharacter,
    favoriteChecks: List<QuickCheckId>,
    onConfigure: () -> Unit,
) {
    Column {
        SectionTitle(
            title = "Избранные проверки",
            trailing = if (favoriteChecks.isEmpty()) "Добавить" else "Настроить",
            onTrailingClick = onConfigure,
        )
        Spacer(Modifier.height(8.dp))

        if (favoriteChecks.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onConfigure),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
            ) {
                Text(
                    text = "Закрепите до четырёх часто используемых проверок и показателей.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                favoriteChecks.chunked(2).forEach { rowChecks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowChecks.forEach { check ->
                            QuickCheckTile(
                                check = check,
                                character = character,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowChecks.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCheckTile(
    check: QuickCheckId,
    character: DublCharacter,
    modifier: Modifier = Modifier,
) {
    val accent = statAccent(check)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = accent.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = check.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = quickCheckValue(check, character),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}

private fun quickCheckValue(check: QuickCheckId, character: DublCharacter): String = when (check) {
    QuickCheckId.DEFENSE -> character.defense.toString()
    QuickCheckId.REFLEXES -> signed(character.reflexes)
    QuickCheckId.INITIATIVE -> signed(character.initiative)
    QuickCheckId.FORTITUDE -> signed(character.fortitude)
    QuickCheckId.RUN -> "${formatNumber(character.runFull)} м"
    QuickCheckId.SIZE -> character.size.toString()
    QuickCheckId.STRENGTH -> character.attribute(AttributeId.STRENGTH).toString()
    QuickCheckId.DEXTERITY -> character.attribute(AttributeId.DEXTERITY).toString()
    QuickCheckId.CONSTITUTION -> character.attribute(AttributeId.CONSTITUTION).toString()
    QuickCheckId.SPEED -> character.attribute(AttributeId.SPEED).toString()
    QuickCheckId.INTELLIGENCE -> character.attribute(AttributeId.INTELLIGENCE).toString()
    QuickCheckId.PERCEPTION -> character.attribute(AttributeId.PERCEPTION).toString()
    QuickCheckId.WILL -> character.attribute(AttributeId.WILL).toString()
    QuickCheckId.CHARISMA -> character.attribute(AttributeId.CHARISMA).toString()
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
    val base = character.attributes[id]?.base ?: character.attributeRaw(id)
    val raw = character.attributeRaw(id)
    val total = character.attribute(id)
    val effectiveModifier = total - base
    val accent = attributeAccent(id)
    val modified = effectiveModifier != 0

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (modified) DublGold.copy(alpha = 0.055f) else accent.copy(alpha = 0.055f),
        border = BorderStroke(
            if (modified) 2.dp else 1.5.dp,
            if (modified) DublGold.copy(alpha = 0.78f) else accent.copy(alpha = 0.72f),
        ),
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
                if (modified) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "База $base · ${signed(effectiveModifier)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = DublGold,
                    )
                } else if (raw != base) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "База $base",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ConditionPickerSheet(
    active: Set<CharacterConditionId>,
    autoWeakness: Boolean,
    onToggle: (CharacterConditionId) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
        ) {
            Text("Состояния", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Выберите активные состояния персонажа. Слабость включается автоматически при нулевой выносливости.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(CharacterConditionId.entries, key = { it.name }) { condition ->
                    val auto = condition == CharacterConditionId.WEAKNESS && autoWeakness
                    val checked = condition in active || auto
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !auto) { onToggle(condition) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (checked) DublAccentSoft.copy(alpha = 0.38f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (checked) DublAccent.copy(alpha = 0.42f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = if (auto) null else { { onToggle(condition) } },
                                enabled = !auto,
                            )
                            Column(Modifier.weight(1f).padding(top = 3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        condition.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (auto) {
                                        Spacer(Modifier.width(7.dp))
                                        Text(
                                            "автоматически",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DublStamina,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    condition.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
private fun FavoriteChecksSheet(
    selected: List<QuickCheckId>,
    onToggle: (QuickCheckId) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
        ) {
            Text("Избранные проверки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Закрепите до четырёх значений. Позже сюда же можно будет добавить умения.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Выбрано ${selected.size} / 4",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected.size >= 4) DublGold else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                item { PickerSectionLabel("Показатели") }
                items(QuickCheckId.entries.take(6), key = { it.name }) { check ->
                    FavoriteCheckRow(check, selected, onToggle)
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    PickerSectionLabel("Характеристики")
                }
                items(QuickCheckId.entries.drop(6), key = { it.name }) { check ->
                    FavoriteCheckRow(check, selected, onToggle)
                }
            }
        }
    }
}

@Composable
private fun PickerSectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = DublGold,
    )
}

@Composable
private fun FavoriteCheckRow(
    check: QuickCheckId,
    selected: List<QuickCheckId>,
    onToggle: (QuickCheckId) -> Unit,
) {
    val checked = check in selected
    val canSelect = checked || selected.size < 4
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = canSelect) { onToggle(check) },
        shape = RoundedCornerShape(9.dp),
        color = if (checked) statAccent(check).copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (checked) statAccent(check).copy(alpha = 0.42f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { if (canSelect) onToggle(check) },
                enabled = canSelect,
            )
            Text(
                text = check.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (canSelect) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.size(width = 22.dp, height = 3.dp),
                shape = RoundedCornerShape(50),
                color = statAccent(check).copy(alpha = if (canSelect) 0.85f else 0.35f),
            ) {}
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
    val base = character.attributes[id]?.base ?: character.attributeRaw(id)
    val total = character.attribute(id)
    val modifier = total - base

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
                color = attributeAccent(id),
            )
            if (modifier != 0) {
                Text(
                    "База $base · текущая модификация ${signed(modifier)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DublGold,
                )
            } else {
                Text(
                    "Базовое значение $base",
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
                supportingText = { Text("Введите урон или лечение целым числом") },
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
