package com.smartcontractai.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class User(
    val id: Int = 0,
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val authType: String = "NORMAL",
    val isCorporate: Boolean = false,
    val taxCode: String? = null,
    val accountType: String = if (isCorporate || authType.startsWith("CORPORATE")) "CORPORATE" else "PERSONAL"
)

data class UserContractStats(
    val myContractsCount: Int = 0,
    val pendingApprovalCount: Int = 0,
    val pendingSignatureCount: Int = 0,
    val completedCount: Int = 0
)

data class UserContractItem(
    val id: Int = 0,
    val title: String,
    val type: String,
    val status: String,
    val userEmail: String,
    val createdAt: String
)

data class PopularTemplateModel(
    val id: String = "1",
    val title: String,
    val usageText: String,
    val category: String = "NDA",
    val usageCount: Int = 0
)


class UserDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "smart_contract_user.db"
        private const val DATABASE_VERSION = 5

        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_PHONE = "phone_number"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_AUTH_TYPE = "auth_type"
        private const val COLUMN_IS_CORPORATE = "is_corporate"
        private const val COLUMN_TAX_CODE = "tax_code"
        private const val COLUMN_ACCOUNT_TYPE = "account_type"

        // Bảng thông báo
        private const val TABLE_NOTIFICATIONS = "notifications"
        private const val COL_NOTIF_ID = "id"
        private const val COL_NOTIF_USER_EMAIL = "user_email"
        private const val COL_NOTIF_TITLE = "title"
        private const val COL_NOTIF_MESSAGE = "message"
        private const val COL_NOTIF_TIME = "time_stamp"
        private const val COL_NOTIF_IS_UNREAD = "is_unread"

        // Bảng thống kê hợp đồng người dùng
        private const val TABLE_CONTRACT_STATS = "user_contract_stats"
        private const val COL_STATS_USER_EMAIL = "user_email"
        private const val COL_MY_CONTRACTS = "my_contracts"
        private const val COL_PENDING_APPROVAL = "pending_approval"
        private const val COL_PENDING_SIGNATURE = "pending_signature"
        private const val COL_COMPLETED = "completed"

        // Bảng hợp đồng
        private const val TABLE_CONTRACTS = "user_contracts"
        private const val COL_CONTRACT_ID = "id"
        private const val COL_CONTRACT_TITLE = "title"
        private const val COL_CONTRACT_TYPE = "type"
        private const val COL_CONTRACT_STATUS = "status"
        private const val COL_CONTRACT_USER_EMAIL = "user_email"
        private const val COL_CONTRACT_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_AUTH_TYPE TEXT DEFAULT 'NORMAL',
                $COLUMN_IS_CORPORATE INTEGER DEFAULT 0,
                $COLUMN_TAX_CODE TEXT,
                $COLUMN_ACCOUNT_TYPE TEXT DEFAULT 'PERSONAL'
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        val createNotificationsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                $COL_NOTIF_ID TEXT PRIMARY KEY,
                $COL_NOTIF_USER_EMAIL TEXT,
                $COL_NOTIF_TITLE TEXT NOT NULL,
                $COL_NOTIF_MESSAGE TEXT NOT NULL,
                $COL_NOTIF_TIME TEXT NOT NULL,
                $COL_NOTIF_IS_UNREAD INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent()
        db.execSQL(createNotificationsTable)

        val createStatsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_CONTRACT_STATS (
                $COL_STATS_USER_EMAIL TEXT PRIMARY KEY,
                $COL_MY_CONTRACTS INTEGER NOT NULL DEFAULT 0,
                $COL_PENDING_APPROVAL INTEGER NOT NULL DEFAULT 0,
                $COL_PENDING_SIGNATURE INTEGER NOT NULL DEFAULT 0,
                $COL_COMPLETED INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createStatsTable)

        val createContractsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_CONTRACTS (
                $COL_CONTRACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CONTRACT_TITLE TEXT NOT NULL,
                $COL_CONTRACT_TYPE TEXT NOT NULL,
                $COL_CONTRACT_STATUS TEXT NOT NULL,
                $COL_CONTRACT_USER_EMAIL TEXT NOT NULL,
                $COL_CONTRACT_CREATED_AT TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createContractsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createNotificationsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                    $COL_NOTIF_ID TEXT PRIMARY KEY,
                    $COL_NOTIF_USER_EMAIL TEXT,
                    $COL_NOTIF_TITLE TEXT NOT NULL,
                    $COL_NOTIF_MESSAGE TEXT NOT NULL,
                    $COL_NOTIF_TIME TEXT NOT NULL,
                    $COL_NOTIF_IS_UNREAD INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent()
            db.execSQL(createNotificationsTable)
        }
        if (oldVersion < 3) {
            val createStatsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_CONTRACT_STATS (
                    $COL_STATS_USER_EMAIL TEXT PRIMARY KEY,
                    $COL_MY_CONTRACTS INTEGER NOT NULL DEFAULT 0,
                    $COL_PENDING_APPROVAL INTEGER NOT NULL DEFAULT 0,
                    $COL_PENDING_SIGNATURE INTEGER NOT NULL DEFAULT 0,
                    $COL_COMPLETED INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent()
            db.execSQL(createStatsTable)
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_AUTH_TYPE TEXT DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_IS_CORPORATE INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_TAX_CODE TEXT")
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_ACCOUNT_TYPE TEXT DEFAULT 'PERSONAL'")
            } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            val createContractsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_CONTRACTS (
                    $COL_CONTRACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_CONTRACT_TITLE TEXT NOT NULL,
                    $COL_CONTRACT_TYPE TEXT NOT NULL,
                    $COL_CONTRACT_STATUS TEXT NOT NULL,
                    $COL_CONTRACT_USER_EMAIL TEXT NOT NULL,
                    $COL_CONTRACT_CREATED_AT TEXT NOT NULL
                )
            """.trimIndent()
            db.execSQL(createContractsTable)
        }
    }

    // Đăng ký người dùng mới vào Database
    fun registerUser(user: User): Boolean {
        val db = writableDatabase
        val isCorp = user.isCorporate || user.accountType == "CORPORATE" || user.authType.startsWith("CORPORATE")
        val values = ContentValues().apply {
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_PHONE, user.phoneNumber)
            put(COLUMN_EMAIL, user.email.trim().lowercase())
            put(COLUMN_PASSWORD, user.password)
            put(COLUMN_AUTH_TYPE, user.authType)
            put(COLUMN_IS_CORPORATE, if (isCorp) 1 else 0)
            put(COLUMN_TAX_CODE, user.taxCode)
            put(COLUMN_ACCOUNT_TYPE, if (isCorp) "CORPORATE" else "PERSONAL")
        }

        return try {
            val result = db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.close()
            result != -1L
        } catch (_: Exception) {
            db.close()
            false
        }
    }

    // Đăng ký tài khoản cá nhân
    fun registerPersonalUser(user: User): Boolean {
        return registerUser(user.copy(isCorporate = false, accountType = "PERSONAL"))
    }

    // Đăng ký tài khoản doanh nghiệp
    fun registerCorporateUser(user: User): Boolean {
        return registerUser(user.copy(isCorporate = true, accountType = "CORPORATE"))
    }

    // Kiểm tra thông tin đăng nhập với Database
    fun checkUserLogin(email: String, password: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(email.trim().lowercase(), password))
        val hasUser = cursor.count > 0
        cursor.close()
        db.close()
        return hasUser
    }

    // Kiểm tra xem Email hoặc SĐT đã tồn tại chưa
    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? OR $COLUMN_PHONE = ?"
        val cursor = db.rawQuery(query, arrayOf(email.trim().lowercase(), email.trim()))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    // Cập nhật mật khẩu mới cho người dùng
    fun updatePassword(emailOrPhone: String, newPassword: String): Boolean {
        val db = writableDatabase
        val cleanInput = emailOrPhone.trim().lowercase()
        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, newPassword)
        }
        val rows = db.update(TABLE_USERS, values, "$COLUMN_EMAIL = ? OR $COLUMN_PHONE = ?", arrayOf(cleanInput, cleanInput))
        db.close()
        return rows > 0
    }

    // Thêm thông báo mới vào Database
    fun saveNotification(id: String, userEmail: String?, title: String, message: String, timeStamp: String, isUnread: Boolean = true): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_ID, id)
            put(COL_NOTIF_USER_EMAIL, userEmail ?: "global")
            put(COL_NOTIF_TITLE, title)
            put(COL_NOTIF_MESSAGE, message)
            put(COL_NOTIF_TIME, timeStamp)
            put(COL_NOTIF_IS_UNREAD, if (isUnread) 1 else 0)
        }
        return try {
            val result = db.insertWithOnConflict(TABLE_NOTIFICATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.close()
            result != -1L
        } catch (_: Exception) {
            db.close()
            false
        }
    }

    // Lấy tất cả thông báo của người dùng từ Database
    fun getAllNotifications(userEmail: String? = null): List<com.smartcontractai.AppNotification> {
        val list = mutableListOf<com.smartcontractai.AppNotification>()
        val db = readableDatabase
        val email = userEmail ?: "global"
        val query = "SELECT * FROM $TABLE_NOTIFICATIONS WHERE $COL_NOTIF_USER_EMAIL = ? OR $COL_NOTIF_USER_EMAIL = 'global' ORDER BY $COL_NOTIF_ID DESC"
        val cursor = db.rawQuery(query, arrayOf(email))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TITLE))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_MESSAGE))
                val time = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TIME))
                val isUnread = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NOTIF_IS_UNREAD)) == 1

                list.add(com.smartcontractai.AppNotification(id, title, message, time, isUnread))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        if (list.isEmpty()) {
            val defaultNotifs = listOf(
                com.smartcontractai.AppNotification(
                    id = "101",
                    title = "AI Copilot đã hoàn tất rà soát",
                    message = "Phân tích rủi ro hợp đồng Thuê văn phòng Q1 đã được lưu tự động vào DB.",
                    time = "21:31",
                    isUnread = true
                ),
                com.smartcontractai.AppNotification(
                    id = "100",
                    title = "Chào mừng bạn đến với SmartContract AI",
                    message = "Hệ thống trợ lý AI đã sẵn sàng hỗ trợ tạo và phân tích rủi ro hợp đồng.",
                    time = "21:30",
                    isUnread = true
                )
            )
            val dbWrite = writableDatabase
            defaultNotifs.forEach { notif ->
                val values = ContentValues().apply {
                    put(COL_NOTIF_ID, notif.id)
                    put(COL_NOTIF_USER_EMAIL, email)
                    put(COL_NOTIF_TITLE, notif.title)
                    put(COL_NOTIF_MESSAGE, notif.message)
                    put(COL_NOTIF_TIME, notif.time)
                    put(COL_NOTIF_IS_UNREAD, if (notif.isUnread) 1 else 0)
                }
                dbWrite.insertWithOnConflict(TABLE_NOTIFICATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            dbWrite.close()
            return defaultNotifs
        }

        return list
    }

    // Đánh dấu 1 thông báo đã đọc
    fun markNotificationAsRead(id: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_IS_UNREAD, 0)
        }
        db.update(TABLE_NOTIFICATIONS, values, "$COL_NOTIF_ID = ?", arrayOf(id))
        db.close()
    }

    // Đánh dấu tất cả thông báo đã đọc
    fun markAllNotificationsAsRead(userEmail: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_IS_UNREAD, 0)
        }
        val email = userEmail ?: "global"
        db.update(TABLE_NOTIFICATIONS, values, "$COL_NOTIF_USER_EMAIL = ? OR $COL_NOTIF_USER_EMAIL = 'global'", arrayOf(email))
        db.close()
    }

    // Lấy thông kê số lượng hợp đồng của từng người dùng từ Database / PostgreSQL
    fun getUserContractStats(userEmail: String?): UserContractStats {
        val cleanEmail = userEmail?.trim()?.lowercase()?.ifBlank { null } ?: "guest@smartcontract.ai"
        val db = readableDatabase

        // 1. Tính toán số liệu thực tế từ danh sách hợp đồng lưu trong bảng user_contracts (nếu có)
        try {
            val contractsQuery = "SELECT $COL_CONTRACT_STATUS, COUNT(*) as cnt FROM $TABLE_CONTRACTS WHERE $COL_CONTRACT_USER_EMAIL = ? GROUP BY $COL_CONTRACT_STATUS"
            val cursorContracts = db.rawQuery(contractsQuery, arrayOf(cleanEmail))

            if (cursorContracts.moveToFirst()) {
                var total = 0
                var pendingApp = 0
                var pendingSig = 0
                var completed = 0

                do {
                    val status = cursorContracts.getString(0) ?: ""
                    val count = cursorContracts.getInt(1)
                    total += count
                    when {
                        status.contains("hoàn tất", ignoreCase = true) || status.contains("đã ký", ignoreCase = true) || status.contains("completed", ignoreCase = true) -> completed += count
                        status.contains("chờ ký", ignoreCase = true) || status.contains("ký", ignoreCase = true) || status.contains("signature", ignoreCase = true) -> pendingSig += count
                        else -> pendingApp += count
                    }
                } while (cursorContracts.moveToNext())
                cursorContracts.close()

                if (total > 0) {
                    val liveStats = UserContractStats(total, pendingApp, pendingSig, completed)
                    saveUserContractStats(cleanEmail, liveStats)
                    db.close()
                    return liveStats
                }
            } else {
                cursorContracts.close()
            }
        } catch (_: Exception) {
            // Trường hợp truy vấn bảng hợp đồng gặp sự cố, chuyển tiếp qua đọc cache từ TABLE_CONTRACT_STATS
        }

        // 2. Lấy dữ liệu thống kê lưu trong bảng user_contract_stats (đã được đồng bộ với PostgreSQL Backend)
        val query = "SELECT * FROM $TABLE_CONTRACT_STATS WHERE $COL_STATS_USER_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(cleanEmail))

        if (cursor.moveToFirst()) {
            val myContracts = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MY_CONTRACTS))
            val pendingApproval = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PENDING_APPROVAL))
            val pendingSignature = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PENDING_SIGNATURE))
            val completed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COMPLETED))
            cursor.close()
            db.close()
            return UserContractStats(myContracts, pendingApproval, pendingSignature, completed)
        }
        cursor.close()
        db.close()

        // 3. Trả về thống kê mặc định (0) nếu tài khoản mới chưa có dữ liệu và lưu lại Database
        val defaultStats = UserContractStats(0, 0, 0, 0)
        saveUserContractStats(cleanEmail, defaultStats)
        return defaultStats
    }

    // Lưu / Cập nhật dữ liệu số lượng hợp đồng của người dùng vào Database
    fun saveUserContractStats(userEmail: String, stats: UserContractStats): Boolean {
        val cleanEmail = userEmail.trim().lowercase()
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATS_USER_EMAIL, cleanEmail)
            put(COL_MY_CONTRACTS, stats.myContractsCount)
            put(COL_PENDING_APPROVAL, stats.pendingApprovalCount)
            put(COL_PENDING_SIGNATURE, stats.pendingSignatureCount)
            put(COL_COMPLETED, stats.completedCount)
        }
        return try {
            val result = db.insertWithOnConflict(TABLE_CONTRACT_STATS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.close()
            result != -1L
        } catch (_: Exception) {
            db.close()
            false
        }
    }

    // Thêm hợp đồng mới vào Database
    fun insertContract(title: String, type: String, status: String, userEmail: String? = null): Boolean {
        val cleanEmail = userEmail?.trim()?.lowercase()?.ifBlank { null } ?: "guest@smartcontract.ai"
        val db = writableDatabase

        val createContractsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_CONTRACTS (
                $COL_CONTRACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CONTRACT_TITLE TEXT NOT NULL,
                $COL_CONTRACT_TYPE TEXT NOT NULL,
                $COL_CONTRACT_STATUS TEXT NOT NULL,
                $COL_CONTRACT_USER_EMAIL TEXT NOT NULL,
                $COL_CONTRACT_CREATED_AT TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createContractsTable)

        val values = ContentValues().apply {
            put(COL_CONTRACT_TITLE, title)
            put(COL_CONTRACT_TYPE, type)
            put(COL_CONTRACT_STATUS, status)
            put(COL_CONTRACT_USER_EMAIL, cleanEmail)
            put(COL_CONTRACT_CREATED_AT, System.currentTimeMillis().toString())
        }

        val result = try {
            db.insert(TABLE_CONTRACTS, null, values)
        } catch (_: Exception) {
            -1L
        }

        if (result != -1L) {
            val currentStats = getUserContractStats(cleanEmail)
            val updatedStats = when {
                status.contains("hoàn tất", ignoreCase = true) || status.contains("đã ký", ignoreCase = true) -> {
                    currentStats.copy(
                        myContractsCount = currentStats.myContractsCount + 1,
                        completedCount = currentStats.completedCount + 1
                    )
                }
                status.contains("ký", ignoreCase = true) -> {
                    currentStats.copy(
                        myContractsCount = currentStats.myContractsCount + 1,
                        pendingSignatureCount = currentStats.pendingSignatureCount + 1
                    )
                }
                else -> {
                    currentStats.copy(
                        myContractsCount = currentStats.myContractsCount + 1,
                        pendingApprovalCount = currentStats.pendingApprovalCount + 1
                    )
                }
            }
            saveUserContractStats(cleanEmail, updatedStats)
        }

        db.close()
        return result != -1L
    }

    // Cập nhật trạng thái hợp đồng trong Database
    fun updateContractStatus(contractId: Int, newStatus: String, userEmail: String? = null): Boolean {
        val cleanEmail = userEmail?.trim()?.lowercase()?.ifBlank { null } ?: "guest@smartcontract.ai"
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_CONTRACT_STATUS, newStatus)
        }

        val rows = db.update(TABLE_CONTRACTS, values, "$COL_CONTRACT_ID = ?", arrayOf(contractId.toString()))
        db.close()

        if (rows > 0) {
            val stats = getUserContractStats(cleanEmail)
            saveUserContractStats(cleanEmail, stats)
        }
        return rows > 0
    }

    // Lấy danh sách hợp đồng gần đây của người dùng từ Database
    fun getRecentContracts(userEmail: String?, limit: Int = 5): List<UserContractItem> {
        val cleanEmail = userEmail?.trim()?.lowercase()?.ifBlank { null } ?: "guest@smartcontract.ai"
        val list = mutableListOf<UserContractItem>()
        val db = readableDatabase

        val createContractsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_CONTRACTS (
                $COL_CONTRACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CONTRACT_TITLE TEXT NOT NULL,
                $COL_CONTRACT_TYPE TEXT NOT NULL,
                $COL_CONTRACT_STATUS TEXT NOT NULL,
                $COL_CONTRACT_USER_EMAIL TEXT NOT NULL,
                $COL_CONTRACT_CREATED_AT TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createContractsTable)

        val query = "SELECT * FROM $TABLE_CONTRACTS WHERE $COL_CONTRACT_USER_EMAIL = ? ORDER BY $COL_CONTRACT_ID DESC LIMIT ?"
        val cursor = db.rawQuery(query, arrayOf(cleanEmail, limit.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CONTRACT_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTRACT_TITLE))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTRACT_TYPE))
                val status = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTRACT_STATUS))
                val email = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTRACT_USER_EMAIL))
                val createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTRACT_CREATED_AT))

                list.add(UserContractItem(id, title, type, status, email, createdAt))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return list
    }

    // Lấy danh sách mẫu hợp đồng phổ biến và số lượt sử dụng từ Database
    fun getPopularTemplates(limit: Int = 2): List<PopularTemplateModel> {
        val list = mutableListOf<PopularTemplateModel>()
        val db = readableDatabase

        val createTemplatesTable = """
            CREATE TABLE IF NOT EXISTS popular_templates (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                usage_text TEXT NOT NULL,
                category TEXT NOT NULL,
                usage_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTemplatesTable)

        val query = "SELECT * FROM popular_templates ORDER BY usage_count DESC LIMIT ?"
        val cursor = db.rawQuery(query, arrayOf(limit.toString()))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val usageText = cursor.getString(cursor.getColumnIndexOrThrow("usage_text"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val usageCount = cursor.getInt(cursor.getColumnIndexOrThrow("usage_count"))

                list.add(PopularTemplateModel(id, title, usageText, category, usageCount))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        if (list.isEmpty()) {
            val defaultList = listOf(
                PopularTemplateModel("1", "Thỏa thuận bảo mật (NDA)", "Dùng 45 lần tuần này", "NDA", 45),
                PopularTemplateModel("2", "Hợp đồng Lao động (Chuẩn)", "Dùng 32 lần tuần này", "Lao động", 32)
            )
            savePopularTemplates(defaultList)
            return defaultList
        }

        return list
    }

    // Lưu / Cập nhật danh sách mẫu phổ biến vào Database
    fun savePopularTemplates(templates: List<PopularTemplateModel>): Boolean {
        val db = writableDatabase
        val createTemplatesTable = """
            CREATE TABLE IF NOT EXISTS popular_templates (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                usage_text TEXT NOT NULL,
                category TEXT NOT NULL,
                usage_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTemplatesTable)

        return try {
            templates.forEach { t ->
                val values = ContentValues().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("usage_text", t.usageText)
                    put("category", t.category)
                    put("usage_count", t.usageCount)
                }
                db.insertWithOnConflict("popular_templates", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.close()
            true
        } catch (_: Exception) {
            db.close()
            false
        }
    }
}

