package com.aibuilt.castlenoads.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibuilt.castlenoads.model.Category
import com.aibuilt.castlenoads.model.Title
import com.aibuilt.castlenoads.ui.CastleViewModel
import com.aibuilt.castlenoads.ui.components.CategoryChips
import com.aibuilt.castlenoads.ui.components.EmptyPanel
import com.aibuilt.castlenoads.ui.components.TitleCard

@Composable
fun SearchScreen(
    vm: CastleViewModel,
    onOpenTitle: (Title) -> Unit
) {
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val titles by vm.titles.collectAsState()

    val results = titles.orEmpty().filter { title ->
        val q = query.trim().lowercase()
        val matches = q.isEmpty() ||
            title.name.lowercase().contains(q) ||
            title.genres.any { it.lowercase().contains(q) } ||
            title.castList.any { it.lowercase().contains(q) } ||
            title.synopsis.lowercase().contains(q)
        val cat = category == null || title.categories.contains(category)
        matches && cat
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Titles, genres, cast") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))
        CategoryChips(selected = category, onSelect = vm::setCategory)
        Spacer(Modifier.height(6.dp))

        if (results.isEmpty()) {
            Box(Modifier.weight(1f)) {
                EmptyPanel("Nothing found", "Try another title, genre or actor.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(results, key = { it.id }) { title ->
                    TitleCard(
                        title = title,
                        onClick = { onOpenTitle(title) },
                        width = 150
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryLabel(category: Category) {
    Text(category.label, style = MaterialTheme.typography.bodyMedium)
}
