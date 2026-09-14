package com.dubl.character.android.model

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SheetGroup(
    val id: String,
    val title: String,
    val itemIds: List<String> = emptyList(),
    val collapsed: Boolean = false,
)

object SheetGroupingRules {
    private const val GROUP_SEPARATOR = "\u001e"
    private const val FIELD_SEPARATOR = "\u001f"
    private const val ITEM_SEPARATOR = "\u001d"

    fun normalize(
        saved: List<SheetGroup>,
        defaults: List<SheetGroup>,
        validItemIds: List<String>,
        ungroupedId: String,
        ungroupedTitle: String = "Без группы",
    ): List<SheetGroup> {
        val valid = validItemIds.toSet()
        if (valid.isEmpty()) {
            return saved.map { it.copy(itemIds = emptyList()) }
        }

        if (saved.isEmpty()) {
            val seen = mutableSetOf<String>()
            val result = defaults.mapNotNull { group ->
                val items = group.itemIds.filter { it in valid && seen.add(it) }
                group.copy(itemIds = items).takeIf { items.isNotEmpty() }
            }.toMutableList()
            val missing = validItemIds.filter { seen.add(it) }
            if (missing.isNotEmpty()) result += SheetGroup(ungroupedId, ungroupedTitle, missing)
            return result
        }

        val seen = mutableSetOf<String>()
        val result = saved.map { group ->
            group.copy(itemIds = group.itemIds.filter { it in valid && seen.add(it) })
        }.toMutableList()
        val missing = validItemIds.filter { seen.add(it) }
        if (missing.isNotEmpty()) {
            val ungroupedIndex = result.indexOfFirst { it.id == ungroupedId }
            if (ungroupedIndex >= 0) {
                val group = result[ungroupedIndex]
                result[ungroupedIndex] = group.copy(itemIds = group.itemIds + missing)
            } else {
                result += SheetGroup(ungroupedId, ungroupedTitle, missing)
            }
        }
        return result
    }

    fun moveItem(
        groups: List<SheetGroup>,
        itemId: String,
        targetGroupId: String,
        targetIndex: Int,
    ): List<SheetGroup> {
        if (groups.none { itemId in it.itemIds }) return groups
        if (groups.none { it.id == targetGroupId }) return groups

        val without = groups.map { group -> group.copy(itemIds = group.itemIds.filterNot { it == itemId }) }.toMutableList()
        val groupIndex = without.indexOfFirst { it.id == targetGroupId }
        val target = without[groupIndex]
        val insertion = targetIndex.coerceIn(0, target.itemIds.size)
        val items = target.itemIds.toMutableList().apply { add(insertion, itemId) }
        without[groupIndex] = target.copy(itemIds = items)
        return without
    }

    /** Move one visual slot. Crossing a group edge moves into the adjacent group. */
    fun moveItemByStep(groups: List<SheetGroup>, itemId: String, direction: Int): List<SheetGroup> {
        if (direction == 0) return groups
        val groupIndex = groups.indexOfFirst { itemId in it.itemIds }
        if (groupIndex < 0) return groups
        val group = groups[groupIndex]
        val itemIndex = group.itemIds.indexOf(itemId)

        return if (direction < 0) {
            when {
                itemIndex > 0 -> moveItem(groups, itemId, group.id, itemIndex - 1)
                groupIndex > 0 -> {
                    val previous = groups[groupIndex - 1]
                    moveItem(groups, itemId, previous.id, previous.itemIds.size)
                }
                else -> groups
            }
        } else {
            when {
                itemIndex < group.itemIds.lastIndex -> moveItem(groups, itemId, group.id, itemIndex + 1)
                groupIndex < groups.lastIndex -> {
                    val next = groups[groupIndex + 1]
                    moveItem(groups, itemId, next.id, 0)
                }
                else -> groups
            }
        }
    }

    fun addGroup(groups: List<SheetGroup>, id: String, title: String): List<SheetGroup> {
        val normalizedTitle = title.trim().ifBlank { "Новая группа" }
        if (groups.any { it.id == id }) return groups
        return groups + SheetGroup(id = id, title = normalizedTitle)
    }

    fun renameGroup(groups: List<SheetGroup>, id: String, title: String): List<SheetGroup> =
        groups.map { group ->
            if (group.id == id) group.copy(title = title.trim().ifBlank { group.title }) else group
        }

    fun toggleCollapsed(groups: List<SheetGroup>, id: String): List<SheetGroup> =
        groups.map { group -> if (group.id == id) group.copy(collapsed = !group.collapsed) else group }

    fun moveGroup(groups: List<SheetGroup>, id: String, direction: Int): List<SheetGroup> {
        val index = groups.indexOfFirst { it.id == id }
        if (index < 0) return groups
        val target = (index + direction.sign()).coerceIn(0, groups.lastIndex)
        if (target == index) return groups
        return groups.toMutableList().apply {
            val group = removeAt(index)
            add(target, group)
        }
    }

    fun deleteGroup(
        groups: List<SheetGroup>,
        id: String,
        ungroupedId: String,
        ungroupedTitle: String = "Без группы",
    ): List<SheetGroup> {
        val removed = groups.firstOrNull { it.id == id } ?: return groups
        if (id == ungroupedId) return groups
        val result = groups.filterNot { it.id == id }.toMutableList()
        if (removed.itemIds.isEmpty()) return result
        val index = result.indexOfFirst { it.id == ungroupedId }
        if (index >= 0) {
            val target = result[index]
            result[index] = target.copy(itemIds = (target.itemIds + removed.itemIds).distinct())
        } else {
            result += SheetGroup(ungroupedId, ungroupedTitle, removed.itemIds)
        }
        return result
    }

    fun encode(groups: List<SheetGroup>): String = groups.joinToString(GROUP_SEPARATOR) { group ->
        listOf(
            encodeToken(group.id),
            encodeToken(group.title),
            if (group.collapsed) "1" else "0",
            group.itemIds.joinToString(ITEM_SEPARATOR) { encodeToken(it) },
        ).joinToString(FIELD_SEPARATOR)
    }

    fun decode(raw: String?): List<SheetGroup> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(GROUP_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            if (fields.size < 4) return@mapNotNull null
            val id = decodeToken(fields[0]).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            SheetGroup(
                id = id,
                title = decodeToken(fields[1]).ifBlank { "Группа" },
                collapsed = fields[2] == "1",
                itemIds = fields[3]
                    .split(ITEM_SEPARATOR)
                    .filter { it.isNotBlank() }
                    .map(::decodeToken)
                    .filter { it.isNotBlank() }
                    .distinct(),
            )
        }
    }

    private fun Int.sign(): Int = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }

    private fun encodeToken(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun decodeToken(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)
}
