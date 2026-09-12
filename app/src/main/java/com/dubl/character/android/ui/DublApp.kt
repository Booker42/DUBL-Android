package com.dubl.character.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dubl.character.android.data.CharacterRepository
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.screens.CharactersScreen
import com.dubl.character.android.ui.screens.OverviewScreen
import com.dubl.character.android.ui.screens.PlaceholderScreen
import com.dubl.character.android.ui.screens.SkillsScreen

private enum class AppSection(val label: String, val glyph: String) {
    OVERVIEW("Персонаж", "П"),
    SKILLS("Умения", "У"),
    MAGIC("Магия", "М"),
    INVENTORY("Инвентарь", "И"),
    MORE("Ещё", "•••"),
}

@Composable
fun DublApp() {
    val appContext = LocalContext.current.applicationContext
    val controller = remember(appContext) {
        CharacterController(CharacterRepository(appContext))
    }
    var selected by rememberSaveable { mutableStateOf(AppSection.OVERVIEW) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppSection.entries.forEach { section ->
                    NavigationBarItem(
                        selected = selected == section,
                        onClick = { selected = section },
                        icon = { Text(section.glyph) },
                        label = { Text(section.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selected) {
                AppSection.OVERVIEW -> OverviewScreen(controller)
                AppSection.SKILLS -> SkillsScreen(controller)
                AppSection.MAGIC -> PlaceholderScreen(
                    "Магия",
                    "Следом перенесём силу магии, школы, уровни школ, заклинания и расход маны.",
                )
                AppSection.INVENTORY -> PlaceholderScreen(
                    "Инвентарь",
                    "Здесь будет нативная экипировка, предметы и быстрые действия без desktop-таблиц.",
                )
                AppSection.MORE -> CharactersScreen(controller)
            }
        }
    }
}
