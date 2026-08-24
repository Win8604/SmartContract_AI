package com.smartcontractai.data

import android.content.Context
import com.smartcontractai.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ContractStatsRepository {

    private val _contractStats = MutableStateFlow(UserContractStats())
    val contractStats: StateFlow<UserContractStats> = _contractStats.asStateFlow()

    // Khởi tạo & nạp thống kê số lượng hợp đồng từ Database cho userEmail hiện tại
    fun loadFromDatabase(context: Context, userEmail: String? = null): UserContractStats {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        val stats = dbHelper.getUserContractStats(email)
        _contractStats.value = stats
        return stats
    }

    // Cập nhật thống kê mới vào Database và tự động phát tín hiệu cập nhật UI real-time
    fun updateStats(context: Context, stats: UserContractStats, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        dbHelper.saveUserContractStats(email, stats)
        _contractStats.value = stats
    }

    // Tự động làm mới dữ liệu từ Database & Đồng bộ trực tiếp với PostgreSQL Backend API
    fun refresh(context: Context, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val localStats = loadFromDatabase(context, email)

        // Đồng bộ dữ liệu số liệu hợp đồng thời gian thực từ PostgreSQL Server nếu có kết nối
        ApiClient.fetchContractStatsFromPostgres(email) { success, myContracts, pendingApproval, pendingSignature, completed ->
            if (success) {
                val postgresStats = UserContractStats(
                    myContractsCount = myContracts,
                    pendingApprovalCount = pendingApproval,
                    pendingSignatureCount = pendingSignature,
                    completedCount = completed
                )
                updateStats(context, postgresStats, email)
            }
        }
    }
}
