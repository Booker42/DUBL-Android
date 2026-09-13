package com.dubl.character.android.ui.components

internal fun sheetContentOverscrollToConsume(
    availableY: Float,
    fromUserInput: Boolean,
): Float = if (fromUserInput) availableY else 0f
