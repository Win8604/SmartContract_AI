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
    val authType: String = "NORMAL"
)

data class UserContractStats(
    val myContractsCount: Int = 0,
    val pendingApprovalCount: Int = 0,
    val pendingSignatureCount: Int = 0,
    val completedCount: Int = 0
)

class UserDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "smart_contract_user.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_PHONE = "phone_number"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"

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
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL
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
    }

    // Đăng ký người dùng mới vào Database
    fun registerUser(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_PHONE, user.phoneNumber)
            put(COLUMN_EMAIL, user.email.trim().lowercase())
            put(COLUMN_PASSWORD, user.password)
        }

        return try {
            val result = db.insert(TABLE_USERS, null, values)
            db.close()
            result != -1L
        } catch (_: Exception) {
            db.close()
            false
        }
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

    // Kiểm tra xem Email đã tồn tại chưa
    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email.trim().lowercase()))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
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

    // Lấy thông kê số lượng hợp đồng của từng người dùng từ Database
    fun getUserContractStats(userEmail: String?): UserContractStats {
        val cleanEmail = userEmail?.trim()?.lowercase()?.ifEmpty { null } ?: "guest@smartcontract.ai"
        val db = readableDatabase
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

        // Nếu chưa có trong Database, tự động tạo dữ liệu mẫu khởi tạo cho từng user và lưu lại vào Database
        val hash = kotlin.math.abs(cleanEmail.hashCode())
        val defaultStats = if (cleanEmail == "guest@smartcontract.ai") {
            UserContractStats(124, 12, 5, 89)
        } else {
            val myContracts = 10 + (hash % 120)
            val pendingApproval = 1 + (hash % 15)
            val pendingSignature = 1 + (hash % 10)
            val completed = (myContracts * 0.7).toInt()
            UserContractStats(myContracts, pendingApproval, pendingSignature, completed)
        }

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
}

