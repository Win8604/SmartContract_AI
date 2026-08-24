package com.smartcontractai.data

import android.content.Context
import com.smartcontractai.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationRepository {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Khởi tạo & nạp toàn bộ thông báo từ SQLite database theo người dùng hiện tại
    fun loadFromDatabase(context: Context, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        val list = dbHelper.getAllNotifications(email)
        _notifications.value = list
    }

    // Đồng bộ thông báo mới từ PostgreSQL Backend Server
    fun refresh(context: Context, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        loadFromDatabase(context, email)

        com.smartcontractai.network.ApiClient.fetchNotificationsFromPostgres(email) { success, items ->
            if (success && items.isNotEmpty()) {
                val dbHelper = UserDatabaseHelper(context.applicationContext)
                items.forEach { item ->
                    dbHelper.saveNotification(item.id, email, item.title, item.message, item.time, item.isUnread)
                }
                _notifications.value = items
            }
        }
    }

    // Tự động thêm thông báo mới vào Database và tự động cập nhật Notification Feed UI
    fun addNotification(context: Context, title: String, message: String, userEmail: String? = null, timeText: String? = null) {
        val time = timeText ?: getCurrentTime()
        val id = System.currentTimeMillis().toString()
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)

        val dbHelper = UserDatabaseHelper(context.applicationContext)
        dbHelper.saveNotification(id, email, title, message, time, isUnread = true)

        val updatedList = dbHelper.getAllNotifications(email)
        _notifications.value = updatedList
    }

    // Tự động cập nhật tất cả thông báo thành đã đọc trong Database, UI và PostgreSQL Backend
    fun markAllAsRead(context: Context, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        dbHelper.markAllNotificationsAsRead(email)
        _notifications.value = _notifications.value.map { it.copy(isUnread = false) }

        com.smartcontractai.network.ApiClient.markAllNotificationsReadOnPostgres(email) { _ -> }
    }

    // Tự động cập nhật 1 thông báo thành đã đọc trong Database và UI
    fun markAsRead(context: Context, id: String) {
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        dbHelper.markNotificationAsRead(id)
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isUnread = false) else it
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
