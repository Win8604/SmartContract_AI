package com.smartcontractai.data

import android.content.Context
import com.smartcontractai.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PopularTemplatesRepository {

    private val _popularTemplates = MutableStateFlow<List<PopularTemplateModel>>(emptyList())
    val popularTemplates: StateFlow<List<PopularTemplateModel>> = _popularTemplates.asStateFlow()

    // Nạp danh sách mẫu phổ biến từ Database
    fun loadFromDatabase(context: Context): List<PopularTemplateModel> {
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        val templates = dbHelper.getPopularTemplates()
        _popularTemplates.value = templates
        return templates
    }

    // Làm mới và đồng bộ danh sách mẫu phổ biến từ PostgreSQL Backend Server
    fun refresh(context: Context) {
        loadFromDatabase(context)

        ApiClient.fetchPopularTemplatesFromPostgres { success, items ->
            if (success && items.isNotEmpty()) {
                val dbHelper = UserDatabaseHelper(context.applicationContext)
                dbHelper.savePopularTemplates(items)
                _popularTemplates.value = items
            }
        }
    }
}
