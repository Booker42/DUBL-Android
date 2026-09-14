package com.dubl.character.android.ui.components

internal fun sheetContentOverscrollToConsume(
    availableY: Float,
    fromUserInput: Boolean,
): Float = if (fromUserInput && availableY < 0f) availableY else 0f

internal fun <T> compactGridRows(items: List<T>, columns: Int = 2): List<List<T>> {
    require(columns > 0) { "columns must be positive" }
    return items.chunked(columns)
}
