package com.dubl.character.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.ui.components.DublCard

@Composable
fun SkillsScreen(character: DublCharacter) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Умения", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск") },
            singleLine = true,
        )
        DublCard(Modifier.fillMaxWidth()) {
            Text("Нативная система умений — следующий модуль", style = MaterialTheme.typography.titleMedium)
            Text(
                "Проверка DUBL: 2d6 + ранг умения + характеристика. В 0.1.4 сюда переносим полный каталог, ранги, стоимость опыта и несколько характеристик на одно умение.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (query.isNotBlank()) {
                Text("Поиск: $query", color = MaterialTheme.colorScheme.primary)
            }
            Text("Текущий персонаж: ${character.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
