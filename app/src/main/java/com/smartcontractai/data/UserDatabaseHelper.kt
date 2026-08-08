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

class UserDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "smart_contract_user.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_PHONE = "phone_number"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
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
        } catch (e: Exception) {
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
}
