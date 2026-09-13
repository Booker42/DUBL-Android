package com.dubl.character.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

@Composable
fun Modifier.containSheetOverscroll(): Modifier {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val consumedY = sheetContentOverscrollToConsume(
                    availableY = available.y,
                    fromUserInput = source == NestedScrollSource.UserInput,
                )
                return if (consumedY == 0f) Offset.Zero else Offset(0f, consumedY)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = if (available.y == 0f) Velocity.Zero else Velocity(0f, available.y)
        }
    }
    return nestedScroll(connection)
}
