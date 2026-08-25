package com.smartcontractai.data

import android.content.Context
import com.smartcontractai.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecentContractsRepository {

    private val _recentContracts = MutableStateFlow<List<UserContractItem>>(emptyList())
    val recentContracts: StateFlow<List<UserContractItem>> = _recentContracts.asStateFlow()

    // Khởi tạo & nạp danh sách hợp đồng gần đây từ Database
    fun loadFromDatabase(context: Context, userEmail: String? = null): List<UserContractItem> {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        val contracts = dbHelper.getRecentContracts(email)
        _recentContracts.value = contracts
        return contracts
    }

    // Làm mới và đồng bộ danh sách hợp đồng với PostgreSQL Backend Server
    fun refresh(context: Context, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        loadFromDatabase(context, email)

        ApiClient.fetchRecentContractsFromPostgres(email) { success, items ->
            if (success && items.isNotEmpty()) {
                _recentContracts.value = items
            }
        }
    }

    // Thêm hợp đồng mới vào Database & cập nhật PostgreSQL + phát tín hiệu real-time cho UI
    fun addContract(context: Context, title: String, type: String, status: String, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        dbHelper.insertContract(title, type, status, email)

        // Cập nhật lên PostgreSQL Backend Server
        ApiClient.createContractOnPostgres(title, type, status, email) { _ -> }

        // Tải lại danh sách hợp đồng & chỉ số thống kê real-time
        loadFromDatabase(context, email)
        ContractStatsRepository.refresh(context, email)
    }

    // Cập nhật trạng thái hợp đồng (Phê duyệt / Từ chối), đồng bộ Database, PostgreSQL Server & UI Realtime
    fun updateContractStatus(context: Context, contractId: Int, newStatus: String, userEmail: String? = null) {
        val email = if (!userEmail.isNullOrBlank()) userEmail else UserFileManager.getCurrentSessionEmail(context)
        val dbHelper = UserDatabaseHelper(context.applicationContext)
        dbHelper.updateContractStatus(contractId, newStatus, email)

        // Cập nhật danh sách trong bộ nhớ real-time để giao diện ẩn ngay lập tức
        _recentContracts.value = _recentContracts.value.map { item ->
            if (item.id == contractId) item.copy(status = newStatus) else item
        }

        // Đồng bộ lên PostgreSQL Backend Server
        if (newStatus.contains("hoàn tất", ignoreCase = true) || newStatus.contains("đã ký", ignoreCase = true) || newStatus.contains("approve", ignoreCase = true)) {
            ApiClient.approveContractOnPostgres(contractId.toString()) { _, _ -> }
        } else {
            ApiClient.rejectContractOnPostgres(contractId.toString()) { _, _ -> }
        }

        // Tải lại dữ liệu từ Database & Thống kê số lượng hợp đồng
        loadFromDatabase(context, email)
        ContractStatsRepository.loadFromDatabase(context, email)
        ContractStatsRepository.refresh(context, email)
    }
}
