package com.aibuilt.castlenoads.data

import android.content.Context
import com.aibuilt.castlenoads.model.Category
import com.aibuilt.castlenoads.model.Title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CatalogRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val _titles = MutableStateFlow<List<Title>?>(null)
    val titles: StateFlow<List<Title>?> = _titles.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    suspend fun load() {
        if (_titles.value != null) return
        _loading.value = true
        _error.value = null
        try {
            val raw = withContext(Dispatchers.IO) {
                context.assets.open("catalog.json").bufferedReader().use { it.readText() }
            }
            delay(250)
            val parsed = json.decodeFromString<List<Title>>(raw)
            if (parsed.isEmpty()) {
                _error.value = "The Castle catalogue came back empty."
            } else {
                _titles.value = parsed
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Could not load the catalogue."
        } finally {
            _loading.value = false
        }
    }

    fun byId(id: String?): Title? = _titles.value?.firstOrNull { it.id == id }

    fun search(query: String, category: Category?): List<Title> {
        val all = _titles.value.orEmpty()
        val q = query.trim().lowercase()
        return all.filter { title ->
            val matchesQuery = q.isEmpty() ||
                title.name.lowercase().contains(q) ||
                title.genres.any { it.lowercase().contains(q) } ||
                title.castList.any { it.lowercase().contains(q) } ||
                title.synopsis.lowercase().contains(q)
            val matchesCategory = category == null || title.categories.contains(category)
            matchesQuery && matchesCategory
        }
    }
}
