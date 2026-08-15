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
    val authType: String = "NORMAL", // NORMAL, GOOGLE, FACEBOOK, CORPORATE, CORPORATE_GOOGLE, CORPORATE_FACEBOOK
    val avatarUrl: String? = null,
    val isCorporate: Boolean = false,
    val taxCode: String? = null,
    val accountType: String = if (isCorporate || authType.startsWith("CORPORATE")) "CORPORATE" else "PERSONAL"
)

object UserFileManager {
    private const val FILE_NAME = "registered_users.json"
    private const val PERSONAL_FILE_NAME = "personal_users.json"
    private const val CORPORATE_FILE_NAME = "corporate_users.json"

    private fun getFile(context: Context, fileName: String = FILE_NAME): File {
        return File(context.filesDir, fileName)
    }

    // Lưu thông tin người dùng vào file JSON (lưu cả vào file chung và file tương ứng cá nhân / doanh nghiệp)
    @Synchronized
    fun saveUser(context: Context, user: UserInfo): Boolean {
        val isCorp = user.isCorporate || user.accountType == "CORPORATE" || user.authType.startsWith("CORPORATE")
        val updatedUser = user.copy(
            isCorporate = isCorp,
            accountType = if (isCorp) "CORPORATE" else "PERSONAL"
        )

        // 1. Lưu vào file danh sách chung registered_users.json
        val saveGeneral = saveToSpecificFile(context, FILE_NAME, updatedUser)

        // 2. Lưu vào file riêng (tài khoản cá nhân hoặc tài khoản doanh nghiệp)
        val targetFileName = if (isCorp) CORPORATE_FILE_NAME else PERSONAL_FILE_NAME
        val saveSpecific = saveToSpecificFile(context, targetFileName, updatedUser)

        return saveGeneral && saveSpecific
    }

    // Lưu tài khoản cá nhân
    @Synchronized
    fun savePersonalUser(context: Context, user: UserInfo): Boolean {
        val personalUser = user.copy(isCorporate = false, accountType = "PERSONAL")
        return saveUser(context, personalUser)
    }

    // Lưu tài khoản doanh nghiệp
    @Synchronized
    fun saveCorporateUser(context: Context, user: UserInfo): Boolean {
        val corpUser = user.copy(isCorporate = true, accountType = "CORPORATE")
        return saveUser(context, corpUser)
    }

    private fun saveToSpecificFile(context: Context, fileName: String, user: UserInfo): Boolean {
        return try {
            val users = getUsersFromFile(context, fileName).toMutableList()
            val cleanEmail = user.email.trim().lowercase()
            val existingIndex = users.indexOfFirst { it.email.lowercase() == cleanEmail }

            if (existingIndex >= 0) {
                // Cập nhật thông tin nếu đã tồn tại
                val oldUser = users[existingIndex]
                val updatedAvatar = if (user.authType.contains("FACEBOOK") || user.avatarUrl != null) user.avatarUrl else oldUser.avatarUrl
                val updatedTaxCode = user.taxCode ?: oldUser.taxCode
                users[existingIndex] = user.copy(email = cleanEmail, avatarUrl = updatedAvatar, taxCode = updatedTaxCode)
            } else {
                // Thêm người dùng mới
                val newId = if (users.isEmpty()) 1 else users.maxOf { it.id } + 1
                users.add(user.copy(id = newId, email = cleanEmail))
            }
            writeUsersToFile(context, fileName, users)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Đọc tất cả thông tin người dùng từ file JSON chỉ định
    @Synchronized
    fun getUsersFromFile(context: Context, fileName: String): List<UserInfo> {
        val file = getFile(context, fileName)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            if (jsonString.isBlank()) return emptyList()

            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<UserInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val isCorp = obj.optBoolean("isCorporate", false) || obj.optString("accountType", "") == "CORPORATE" || obj.optString("authType", "").startsWith("CORPORATE")
                list.add(
                    UserInfo(
                        id = obj.optInt("id", 0),
                        fullName = obj.optString("fullName", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        email = obj.optString("email", ""),
                        password = obj.optString("password", ""),
                        authType = obj.optString("authType", "NORMAL"),
                        avatarUrl = obj.optString("avatarUrl", "").ifEmpty { null },
                        isCorporate = isCorp,
                        taxCode = obj.optString("taxCode", "").ifEmpty { null },
                        accountType = if (isCorp) "CORPORATE" else "PERSONAL"
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Đọc tất cả thông tin người dùng từ file JSON chung
    @Synchronized
    fun getAllUsers(context: Context): List<UserInfo> {
        return getUsersFromFile(context, FILE_NAME)
    }

    // Lấy danh sách tài khoản cá nhân
    @Synchronized
    fun getPersonalUsers(context: Context): List<UserInfo> {
        return getUsersFromFile(context, PERSONAL_FILE_NAME)
    }

    // Lấy danh sách tài khoản doanh nghiệp
    @Synchronized
    fun getCorporateUsers(context: Context): List<UserInfo> {
        return getUsersFromFile(context, CORPORATE_FILE_NAME)
    }

    // Lấy thông tin user theo email
    fun getUserByEmail(context: Context, email: String): UserInfo? {
        if (email.isBlank()) return null
        val cleanEmail = email.trim().lowercase()
        return getAllUsers(context).firstOrNull { it.email.lowercase() == cleanEmail }
    }

    private fun writeUsersToFile(context: Context, fileName: String, users: List<UserInfo>) {
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
                put("isCorporate", user.isCorporate)
                put("taxCode", user.taxCode ?: "")
                put("accountType", user.accountType)
            }
            jsonArray.put(obj)
        }
        getFile(context, fileName).writeText(jsonArray.toString(4))
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
