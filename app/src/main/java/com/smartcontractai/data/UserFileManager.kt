package com.smartcontractai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class UserInfo(
    val id: Int = 0,
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val authType: String = "NORMAL", // NORMAL, GOOGLE, FACEBOOK
    val avatarUrl: String? = null
)

object UserFileManager {
    private const val FILE_NAME = "registered_users.json"

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    // Lưu thông tin người dùng vào file JSON
    @Synchronized
    fun saveUser(context: Context, user: UserInfo): Boolean {
        return try {
            val users = getAllUsers(context).toMutableList()
            val cleanEmail = user.email.trim().lowercase()
            val existingIndex = users.indexOfFirst { it.email.lowercase() == cleanEmail }

            if (existingIndex >= 0) {
                // Cập nhật thông tin nếu đã tồn tại
                val oldUser = users[existingIndex]
                val updatedAvatar = if (user.authType == "FACEBOOK" || user.avatarUrl != null) user.avatarUrl else oldUser.avatarUrl
                users[existingIndex] = user.copy(email = cleanEmail, avatarUrl = updatedAvatar)
            } else {
                // Thêm người dùng mới
                val newId = if (users.isEmpty()) 1 else users.maxOf { it.id } + 1
                users.add(user.copy(id = newId, email = cleanEmail))
            }
            writeUsersToFile(context, users)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Đọc tất cả thông tin người dùng từ file JSON
    @Synchronized
    fun getAllUsers(context: Context): List<UserInfo> {
        val file = getFile(context)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            if (jsonString.isBlank()) return emptyList()

            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<UserInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    UserInfo(
                        id = obj.optInt("id", 0),
                        fullName = obj.optString("fullName", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        email = obj.optString("email", ""),
                        password = obj.optString("password", ""),
                        authType = obj.optString("authType", "NORMAL"),
                        avatarUrl = obj.optString("avatarUrl", "").ifEmpty { null }
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Lấy thông tin user theo email
    fun getUserByEmail(context: Context, email: String): UserInfo? {
        if (email.isBlank()) return null
        val cleanEmail = email.trim().lowercase()
        return getAllUsers(context).firstOrNull { it.email.lowercase() == cleanEmail }
    }

    private fun writeUsersToFile(context: Context, users: List<UserInfo>) {
        val jsonArray = JSONArray()
        for (user in users) {
            val obj = JSONObject().apply {
                put("id", user.id)
                put("fullName", user.fullName)
                put("phoneNumber", user.phoneNumber)
                put("email", user.email.trim().lowercase())
                put("password", user.password)
                put("authType", user.authType)
                put("avatarUrl", user.avatarUrl ?: "")
            }
            jsonArray.put(obj)
        }
        getFile(context).writeText(jsonArray.toString(4))
    }

    // Kiểm tra xem Email đã tồn tại trong file chưa
    fun isEmailExists(context: Context, email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return getAllUsers(context).any { it.email.lowercase() == cleanEmail }
    }

    // Kiểm tra thông tin Đăng Nhập tài khoản thường với file
    fun checkNormalLogin(context: Context, email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return getAllUsers(context).any {
            it.email.lowercase() == cleanEmail && it.password == password
        }
    }

    // Kiểm tra tài khoản người dùng có tồn tại theo Email không
    fun checkUserExists(context: Context, email: String): Boolean {
        return isEmailExists(context, email)
    }

    private const val PREF_NAME = "user_session_pref"
    private const val KEY_CURRENT_EMAIL = "current_email"

    // Lưu email phiên làm việc của người dùng hiện tại
    fun saveCurrentSessionEmail(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_EMAIL, email.trim().lowercase()).apply()
    }

    // Lấy email người dùng đang đăng nhập hiện tại
    fun getCurrentSessionEmail(context: Context): String {
        val firebaseEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
        if (!firebaseEmail.isNullOrBlank()) return firebaseEmail.trim().lowercase()

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_EMAIL, "") ?: ""
    }
}
