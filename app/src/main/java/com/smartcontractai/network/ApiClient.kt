package com.smartcontractai.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ShoppingBag
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class TemplateCategoryItem(
    val key: String,
    val name: String
)

object ApiClient {
    // Địa chỉ Backend khi chạy trên Android Emulator (10.0.2.2 tương đương localhost)
    const val BASE_URL = "http://10.0.2.2:5000/api/v1"

    private val client = OkHttpClient()

    /**
     * Đồng bộ danh sách Danh mục mẫu hợp đồng từ PostgreSQL Backend Server
     */
    fun fetchTemplateCategoriesFromPostgres(onResult: (Boolean, List<TemplateCategoryItem>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/templates/categories")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val jsonArray = parseJsonArray(body)
                        val list = mutableListOf<TemplateCategoryItem>()
                        list.add(TemplateCategoryItem(key = "all", name = "Tất cả"))
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val key = obj.optString("key", "")
                            val name = obj.optString("name", key)
                            if (key.isNotEmpty() && list.none { it.key == key }) {
                                list.add(TemplateCategoryItem(key = key, name = name))
                            }
                        }
                        onResult(true, list)
                    } catch (_: Exception) {
                        onResult(false, emptyList())
                    }
                } else {
                    onResult(false, emptyList())
                }
            }
        })
    }

    private fun parseJsonArray(bodyStr: String): JSONArray {
        val trimmed = bodyStr.trim()
        if (trimmed.startsWith("[")) {
            return JSONArray(trimmed)
        }
        if (trimmed.startsWith("{")) {
            val jsonObj = JSONObject(trimmed)
            return jsonObj.optJSONArray("data") ?: JSONArray()
        }
        return JSONArray()
    }

    fun syncUserWithBackend(idToken: String, fullName: String?, provider: String, onResult: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply {
            put("fullName", fullName ?: "")
            put("provider", provider)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$BASE_URL/auth/sync-user")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $idToken")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                onResult(response.isSuccessful, body)
            }
        })
    }

    /**
     * Đồng bộ và lấy số liệu thống kê hợp đồng mới nhất từ PostgreSQL Backend Server
     */
    fun fetchContractStatsFromPostgres(userEmail: String?, onResult: (Boolean, Int, Int, Int, Int) -> Unit) {
        val emailParam = userEmail?.trim()?.lowercase() ?: ""
        val request = Request.Builder()
            .url("$BASE_URL/contracts/stats?userEmail=$emailParam")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, 0, 0, 0, 0)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val rootJson = JSONObject(body)
                        val json = if (rootJson.has("data") && rootJson.optJSONObject("data") != null) {
                            rootJson.getJSONObject("data")
                        } else {
                            rootJson
                        }
                        val myContracts = json.optInt("myContractsCount", json.optInt("my_contracts", json.optInt("myContracts", 0)))
                        val pendingApproval = json.optInt("pendingApprovalCount", json.optInt("pending_approval", json.optInt("pendingInternalReview", 0)))
                        val pendingSignature = json.optInt("pendingSignatureCount", json.optInt("pending_signature", json.optInt("pendingSignature", 0)))
                        val completed = json.optInt("completedCount", json.optInt("completed", 0))
                        onResult(true, myContracts, pendingApproval, pendingSignature, completed)
                    } catch (_: Exception) {
                        onResult(false, 0, 0, 0, 0)
                    }
                } else {
                    onResult(false, 0, 0, 0, 0)
                }
            }
        })
    }

    /**
     * Đồng bộ danh sách Hợp đồng gần đây từ PostgreSQL Backend Server
     */
    fun fetchRecentContractsFromPostgres(userEmail: String?, onResult: (Boolean, List<com.smartcontractai.data.UserContractItem>) -> Unit) {
        val emailParam = userEmail?.trim()?.lowercase() ?: ""
        val request = Request.Builder()
            .url("$BASE_URL/contracts/recent?userEmail=$emailParam")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val jsonArray = parseJsonArray(body)
                        val list = mutableListOf<com.smartcontractai.data.UserContractItem>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            list.add(
                                com.smartcontractai.data.UserContractItem(
                                    id = obj.optInt("id", 0),
                                    title = obj.optString("title", "Hợp đồng"),
                                    type = obj.optString("type", "Mẫu chuẩn"),
                                    status = obj.optString("status", "Đang rà soát"),
                                    userEmail = obj.optString("userEmail", emailParam),
                                    createdAt = obj.optString("createdAt", System.currentTimeMillis().toString())
                                )
                            )
                        }
                        onResult(true, list)
                    } catch (_: Exception) {
                        onResult(false, emptyList())
                    }
                } else {
                    onResult(false, emptyList())
                }
            }
        })
    }

    /**
     * Đồng bộ thêm hợp đồng mới lên PostgreSQL Backend Server
     */
    fun createContractOnPostgres(title: String, type: String, status: String, userEmail: String?, onResult: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("title", title)
            put("type", type)
            put("status", status)
            put("userEmail", userEmail ?: "")
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/contracts/create")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful)
            }
        })
    }

    /**
     * Đồng bộ danh sách Mẫu phổ biến và lượt sử dụng từ PostgreSQL Backend Server
     */
    fun fetchPopularTemplatesFromPostgres(onResult: (Boolean, List<com.smartcontractai.data.PopularTemplateModel>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/templates/popular")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val jsonArray = parseJsonArray(body)
                        val list = mutableListOf<com.smartcontractai.data.PopularTemplateModel>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val usage = obj.optInt("downloads_count", obj.optInt("usageCount", obj.optInt("usage_count", 0)))
                            val cat = obj.optString("category", "rental")
                            val mappedCat = when (cat.lowercase()) {
                                "rental" -> "Thuê nhà"
                                "deposit" -> "Đặt cọc"
                                "commercial" -> "Kinh doanh"
                                "annex" -> "Phụ lục"
                                else -> cat
                            }
                            list.add(
                                com.smartcontractai.data.PopularTemplateModel(
                                    id = obj.optString("id", i.toString()),
                                    title = obj.optString("title", "Mẫu hợp đồng"),
                                    usageText = "Dùng $usage lần",
                                    category = mappedCat,
                                    usageCount = usage
                                )
                            )
                        }
                        onResult(true, list)
                    } catch (_: Exception) {
                        onResult(false, emptyList())
                    }
                } else {
                    onResult(false, emptyList())
                }
            }
        })
    }

    fun getDefaultBackendTemplates(category: String? = null): List<com.smartcontractai.ContractTemplateModel> {
        val catKey = when (category?.trim()?.lowercase()) {
            "thuê nhà", "rental" -> "rental"
            "đặt cọc", "deposit" -> "deposit"
            "mặt bằng kinh doanh", "commercial" -> "commercial"
            "phụ lục", "annex" -> "annex"
            else -> null
        }

        val all = listOf(
            com.smartcontractai.ContractTemplateModel(
                id = "1",
                code = "TMPL_NHA_TRO_STANDARD",
                title = "Hợp đồng thuê phòng trọ chuẩn quy định",
                description = "Mẫu hợp đồng thuê phòng trọ cá nhân chuẩn pháp lý, bao gồm các điều khoản về tiền đặt cọc, giá điện nước, giờ giấc và trách nhiệm giữ gìn an ninh trật tự.",
                category = "Thuê nhà",
                scope = "Công khai",
                badgeText = "✨ AI Tối ưu",
                isAiOptimized = true,
                usageCount = "152 lượt dùng",
                timeAgo = "Vừa cập nhật",
                icon = Icons.Outlined.Apartment,
                isPrimaryButton = true,
                templateContent = "CỘNG HOÀ XÃ HỘI CHỦ NGHĨA VIỆT NAM\nĐộc lập - Tự do - Hạnh phúc\n\nHỢP ĐỒNG THUÊ PHÒNG TRỌ\n..."
            ),
            com.smartcontractai.ContractTemplateModel(
                id = "2",
                code = "TMPL_DAT_COC_PHONG_TRO",
                title = "Hợp đồng đặt cọc giữ chỗ phòng trọ",
                description = "Mẫu hợp đồng đặt cọc giữ chỗ phòng trọ trong thời gian chờ nhận phòng, cam kết giữ phòng và bảo toàn tiền cọc đúng hạn.",
                category = "Đặt cọc",
                scope = "Công khai",
                badgeText = "✨ AI Tối ưu",
                isAiOptimized = true,
                usageCount = "89 lượt dùng",
                timeAgo = "Vừa cập nhật",
                icon = Icons.Outlined.EditNote,
                templateContent = "HỢP ĐỒNG ĐẶT CỌC GIỮ CHỖ THUÊ PHÒNG\n..."
            ),
            com.smartcontractai.ContractTemplateModel(
                id = "3",
                code = "TMPL_MAT_BANG_KINH_DOANH",
                title = "Hợp đồng cho thuê mặt bằng / Nhà nguyên căn",
                description = "Mẫu hợp đồng thuê nhà nguyên căn, căn hộ dịch vụ hoặc mặt bằng kinh doanh dài hạn với điều khoản bảo trì tài sản và thanh toán định kỳ.",
                category = "Mặt bằng kinh doanh",
                scope = "Doanh nghiệp",
                badgeText = "Mặt bằng kinh doanh",
                isAiOptimized = false,
                usageCount = "45 lượt dùng",
                timeAgo = "Vừa cập nhật",
                icon = Icons.Outlined.ShoppingBag,
                templateContent = "HỢP ĐỒNG CHO THUÊ NHÀ NGUYÊN CĂN & MẶT BẰNG\n..."
            ),
            com.smartcontractai.ContractTemplateModel(
                id = "4",
                code = "TMPL_PHU_LUC_GIA_HAN",
                title = "Phụ lục gia hạn hợp đồng thuê nhà",
                description = "Mẫu phụ lục gia hạn thời hạn hợp đồng đã ký kết mà không cần soạn thảo lại toàn bộ điều khoản từ đầu.",
                category = "Phụ lục",
                scope = "Cá nhân",
                badgeText = "Phụ lục",
                isAiOptimized = false,
                usageCount = "34 lượt dùng",
                timeAgo = "Vừa cập nhật",
                icon = Icons.Outlined.Badge,
                templateContent = "PHỤ LỤC GIA HẠN HỢP ĐỒNG THUÊ\n..."
            )
        )

        if (catKey == null) return all

        return all.filter { tmpl ->
            when (catKey) {
                "rental" -> tmpl.category == "Thuê nhà"
                "deposit" -> tmpl.category == "Đặt cọc"
                "commercial" -> tmpl.category == "Mặt bằng kinh doanh"
                "annex" -> tmpl.category == "Phụ lục"
                else -> true
            }
        }
    }

    /**
     * Đồng bộ danh sách Thư viện mẫu hợp đồng từ PostgreSQL Backend Server (template.routes.ts, template.types.ts)
     */
    fun fetchTemplatesFromPostgres(
        category: String? = null,
        search: String? = null,
        onResult: (Boolean, List<com.smartcontractai.ContractTemplateModel>) -> Unit
    ) {
        val urlBuilder = StringBuilder("$BASE_URL/templates")
        val params = mutableListOf<String>()
        val catKey = when (category?.trim()?.lowercase()) {
            "thuê nhà", "rental" -> "rental"
            "đặt cọc", "deposit" -> "deposit"
            "mặt bằng kinh doanh", "commercial" -> "commercial"
            "phụ lục", "annex" -> "annex"
            else -> category
        }
        if (!catKey.isNullOrBlank() && catKey != "all" && catKey != "Tất cả") {
            params.add("category=${java.net.URLEncoder.encode(catKey, "UTF-8")}")
        }
        if (!search.isNullOrBlank()) {
            params.add("search=${java.net.URLEncoder.encode(search, "UTF-8")}")
        }
        if (params.isNotEmpty()) {
            urlBuilder.append("?").append(params.joinToString("&"))
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, getDefaultBackendTemplates(catKey))
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string() ?: ""
                        val jsonArray = parseJsonArray(body)
                        val list = mutableListOf<com.smartcontractai.ContractTemplateModel>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.optString("id", i.toString())
                            val code = obj.optString("code", "")
                            val title = obj.optString("title", "Mẫu hợp đồng")
                            val cat = obj.optString("category", "rental")
                            val desc = obj.optString("description", "")
                            val templateContent = obj.optString("template_content", "")
                            val isOfficial = obj.optBoolean("is_official", true)
                            val downloads = obj.optInt("downloads_count", obj.optInt("downloadsCount", 0))

                            val mappedCategoryName = when (cat.lowercase()) {
                                "rental" -> "Thuê nhà"
                                "deposit" -> "Đặt cọc"
                                "commercial" -> "Mặt bằng kinh doanh"
                                "annex" -> "Phụ lục"
                                else -> cat
                            }

                            val icon = when (cat.lowercase()) {
                                "rental" -> Icons.Outlined.Apartment
                                "deposit" -> Icons.Outlined.EditNote
                                "commercial" -> Icons.Outlined.ShoppingBag
                                "annex" -> Icons.Outlined.Badge
                                else -> Icons.Outlined.EditNote
                            }

                            val badgeText = if (isOfficial) "✨ AI Tối ưu" else mappedCategoryName

                            list.add(
                                com.smartcontractai.ContractTemplateModel(
                                    id = id,
                                    title = title,
                                    description = desc,
                                    category = mappedCategoryName,
                                    scope = if (isOfficial) "Công khai" else "Doanh nghiệp",
                                    badgeText = badgeText,
                                    isAiOptimized = isOfficial,
                                    usageCount = "$downloads lượt dùng",
                                    timeAgo = "Vừa cập nhật",
                                    icon = icon,
                                    isPrimaryButton = (i == 0),
                                    code = code,
                                    templateContent = templateContent
                                )
                            )
                        }
                        if (list.isEmpty()) {
                            onResult(true, getDefaultBackendTemplates(catKey))
                        } else {
                            onResult(true, list)
                        }
                    } catch (_: Exception) {
                        onResult(false, getDefaultBackendTemplates(catKey))
                    }
                } else {
                    onResult(false, getDefaultBackendTemplates(catKey))
                }
            }
        })
    }

    private fun formatNotificationTime(rawTime: String): String {
        if (rawTime.isBlank()) return "Vừa xong"
        if (rawTime.contains(":") && rawTime.length <= 8 && !rawTime.contains("T")) return rawTime
        return try {
            if (rawTime.contains("T")) {
                val timePart = rawTime.substringAfter("T").substringBefore(".")
                val parts = timePart.split(":")
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else rawTime
            } else {
                rawTime
            }
        } catch (_: Exception) {
            "Vừa xong"
        }
    }

    /**
     * Đồng bộ danh sách thông báo từ PostgreSQL Backend Server
     */
    fun fetchNotificationsFromPostgres(userEmail: String?, onResult: (Boolean, List<com.smartcontractai.AppNotification>) -> Unit) {
        val emailParam = userEmail?.trim()?.lowercase() ?: ""
        val request = Request.Builder()
            .url("$BASE_URL/notifications?userEmail=$emailParam")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    try {
                        val bodyStr = response.body?.string() ?: ""
                        val jsonArray = parseJsonArray(bodyStr)
                        val list = mutableListOf<com.smartcontractai.AppNotification>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val title = obj.optString("title", "Thông báo")
                            val message = obj.optString("body", obj.optString("message", ""))
                            val isRead = if (obj.has("is_read")) obj.optBoolean("is_read") else if (obj.has("isUnread")) !obj.optBoolean("isUnread") else false
                            val rawTime = obj.optString("created_at", obj.optString("time", ""))
                            val timeStr = formatNotificationTime(rawTime)

                            list.add(
                                com.smartcontractai.AppNotification(
                                    id = obj.optString("id", i.toString()),
                                    title = title,
                                    message = message,
                                    time = timeStr,
                                    isUnread = !isRead
                                )
                            )
                        }
                        onResult(true, list)
                    } catch (_: Exception) {
                        onResult(false, emptyList())
                    }
                } else {
                    onResult(false, emptyList())
                }
            }
        })
    }

    /**
     * Cập nhật tất cả thông báo thành đã đọc trên PostgreSQL Backend Server
     */
    fun markAllNotificationsReadOnPostgres(userEmail: String?, onResult: (Boolean) -> Unit) {
        val json = JSONObject().apply {
            put("userEmail", userEmail ?: "")
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/notifications/read-all")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful)
            }
        })
    }

    /**
     * Đăng ký tài khoản người dùng trực tiếp vào Database schema.sql bên Backend PostgreSQL Server
     */
    fun registerUserOnBackend(
        fullName: String,
        email: String,
        password: String,
        phoneNumber: String?,
        accountType: String = "personal",
        onResult: (Boolean, String?) -> Unit
    ) {
        val json = JSONObject().apply {
            put("fullName", fullName)
            put("email", email.trim().lowercase())
            put("password", password)
            if (!phoneNumber.isNullOrBlank()) {
                put("phoneNumber", phoneNumber)
            }
            val accType = if (accountType.lowercase() == "corporate" || accountType.lowercase() == "business") "business" else "personal"
            put("accountType", accType)
            put("role", "landlord")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/auth/register")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, "Không thể kết nối đến máy chủ Backend (Lỗi mạng)")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val jsonObj = JSONObject(bodyStr)
                        val message = jsonObj.optString("message", "Đăng ký tài khoản thành công")
                        onResult(true, message)
                    } catch (_: Exception) {
                        onResult(true, "Đăng ký tài khoản thành công")
                    }
                } else {
                    val errMsg = try {
                        val jsonObj = JSONObject(bodyStr)
                        jsonObj.optString("message", "Đăng ký tài khoản thất bại")
                    } catch (_: Exception) {
                        "Đăng ký tài khoản thất bại (HTTP ${response.code})"
                    }
                    onResult(false, errMsg)
                }
            }
        })
    }

    /**
     * Đăng nhập và kiểm tra thông tin người dùng từ Database schema.sql bên Backend PostgreSQL Server
     */
    fun loginUserOnBackend(
        email: String,
        password: String,
        onResult: (Boolean, String?, JSONObject?) -> Unit
    ) {
        val json = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("password", password)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, "Mất kết nối máy chủ Backend", null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val jsonObj = JSONObject(bodyStr)
                        val dataObj = jsonObj.optJSONObject("data")
                        val message = jsonObj.optString("message", "Đăng nhập thành công")
                        onResult(true, message, dataObj)
                    } catch (_: Exception) {
                        onResult(true, "Đăng nhập thành công", null)
                    }
                } else {
                    val errMsg = try {
                        val jsonObj = JSONObject(bodyStr)
                        jsonObj.optString("message", "Email hoặc mật khẩu không chính xác")
                    } catch (_: Exception) {
                        "Đăng nhập thất bại (HTTP ${response.code})"
                    }
                    onResult(false, errMsg, null)
                }
            }
        })
    }

    /**
     * Phê duyệt hợp đồng trên PostgreSQL Backend Server
     */
    fun approveContractOnPostgres(contractId: String, onResult: (Boolean, String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/contracts/$contractId/approve")
            .post(JSONObject().toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful, response.body?.string())
            }
        })
    }

    /**
     * Từ chối hợp đồng trên PostgreSQL Backend Server
     */
    fun rejectContractOnPostgres(contractId: String, reason: String? = null, onResult: (Boolean, String?) -> Unit) {
        val json = JSONObject().apply { put("reason", reason ?: "Không đạt yêu cầu") }
        val request = Request.Builder()
            .url("$BASE_URL/contracts/$contractId/reject")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful, response.body?.string())
            }
        })
    }

    /**
     * Lấy nhật ký hoạt động Audit Logs từ PostgreSQL Backend Server
     */
    fun fetchAuditLogsFromPostgres(onResult: (Boolean, List<AuditLogApiItem>) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/contracts/audit-logs")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onResult(false, emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    try {
                        val jsonObj = JSONObject(bodyStr)
                        val dataArray = jsonObj.optJSONArray("data")
                        val list = mutableListOf<AuditLogApiItem>()
                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                list.add(
                                    AuditLogApiItem(
                                        id = item.optString("id", i.toString()),
                                        text = item.optString("text"),
                                        time = item.optString("time"),
                                        colorHex = item.optString("color", "#2563EB")
                                    )
                                )
                            }
                        }
                        onResult(true, list)
                    } catch (_: Exception) {
                        onResult(false, emptyList())
                    }
                } else {
                    onResult(false, emptyList())
                }
            }
        })
    }
}

data class AuditLogApiItem(
    val id: String,
    val text: String,
    val time: String,
    val colorHex: String
)

