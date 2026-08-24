@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.data.RecentContractsRepository
import com.smartcontractai.data.UserContractItem
import com.smartcontractai.data.UserDatabaseHelper
import com.smartcontractai.data.UserFileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneContractBottomSheet(
    onDismissRequest: () -> Unit,
    onContractCloned: (newTitle: String, newContent: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userEmail = remember { UserFileManager.getCurrentSessionEmail(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Lấy danh sách hợp đồng đã tạo từ Database & Repository
    val contractsList = remember(userEmail) {
        val dbHelper = UserDatabaseHelper(context)
        dbHelper.getRecentContracts(userEmail, limit = 50)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedContract by remember { mutableStateOf<UserContractItem?>(contractsList.firstOrNull()) }
    var clonedTitleText by remember { mutableStateOf(selectedContract?.let { "Bản sao - ${it.title}" } ?: "") }
    var isCloning by remember { mutableStateOf(false) }

    // Cập nhật tên hợp đồng khi chọn hợp đồng khác
    LaunchedEffect(selectedContract) {
        selectedContract?.let {
            if (clonedTitleText.isEmpty() || clonedTitleText.startsWith("Bản sao - ")) {
                clonedTitleText = "Bản sao - ${it.title}"
            }
        }
    }

    val filteredContracts = remember(contractsList, searchQuery) {
        if (searchQuery.isBlank()) {
            contractsList
        } else {
            contractsList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.type.contains(searchQuery, ignoreCase = true) ||
                        it.status.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Nhân bản hợp đồng",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Chọn hợp đồng đã có để sao chép nhanh (10s)",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ô tìm kiếm hợp đồng
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm kiếm hợp đồng theo tên, loại...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1D4ED8),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Danh sách hợp đồng sẵn có (${filteredContracts.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Danh sách hợp đồng chọn
            if (filteredContracts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Không tìm thấy hợp đồng phù hợp" else "Chưa có hợp đồng nào trong danh sách",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContracts, key = { it.id }) { item ->
                        val isSelected = selectedContract?.id == item.id
                        ContractItemCard(
                            contract = item,
                            isSelected = isSelected,
                            onSelect = {
                                selectedContract = item
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ô nhập tên hợp đồng mới (nếu đã chọn)
            AnimatedVisibility(
                visible = selectedContract != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Text(
                        text = "Tên hợp đồng mới nhân bản",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = clonedTitleText,
                        onValueChange = { clonedTitleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1D4ED8),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Nút bấm Nhân bản
            Button(
                onClick = {
                    val contract = selectedContract
                    if (contract == null) {
                        Toast.makeText(context, "Vui lòng chọn 1 hợp đồng để nhân bản", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalTitle = clonedTitleText.trim().ifEmpty { "Bản sao - ${contract.title}" }

                    isCloning = true
                    scope.launch {
                        // Giả lập tiến trình nhân bản AI / DB nhanh (500ms)
                        delay(600)
                        // Thêm hợp đồng mới vào database & repository
                        RecentContractsRepository.addContract(
                            context = context,
                            title = finalTitle,
                            type = contract.type,
                            status = "Đang rà soát",
                            userEmail = userEmail
                        )

                        val sampleContent = """
                            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
                            Độc lập - Tự do - Hạnh phúc
                            -------------------

                            ${finalTitle.uppercase()}
                            (Bản nhân bản từ: ${contract.title})

                            Hôm nay, ngày ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())}, chúng tôi gồm có:

                            BÊN A (Bên giao): SMARTCONTRACT AI SYSTEM
                            BÊN B (Bên nhận): NGUYỄN VĂN A

                            ĐIỀU 1: NỘI DUNG NGHĨA VỤ VÀ ĐIỀU KHOẢN HỢP ĐỒNG
                            1.1. Bản sao này thừa hưởng đầy đủ toàn bộ cấu trúc và các điều khoản pháp lý tiêu chuẩn từ hợp đồng gốc "${contract.title}".
                            1.2. Các thông tin về thời gian, địa điểm và điều khoản giá trị được giữ nguyên để phục vụ chỉnh sửa chi tiết.

                            ĐIỀU 2: HIỆU LỰC HỢP ĐỒNG
                            Hợp đồng này có hiệu lực kể từ ngày ký và được tạo thông qua tính năng Nhân bản nhanh (10s) của SmartContract AI.
                        """.trimIndent()

                        isCloning = false
                        Toast.makeText(context, "Đã nhân bản hợp đồng thành công!", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                        onContractCloned(finalTitle, sampleContent)
                    }
                },
                enabled = selectedContract != null && !isCloning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1D4ED8),
                    disabledContainerColor = Color(0xFF94A3B8)
                )
            ) {
                if (isCloning) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Đang nhân bản...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nhân bản", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ContractItemCard(
    contract: UserContractItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE2E8F0)
    val bgColor = if (isSelected) Color(0xFFEFF6FF) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF1D4ED8),
                    unselectedColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contract.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = contract.type,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Status Tag
                    val (statusBg, statusTextColor) = when {
                        contract.status.contains("hoàn tất", ignoreCase = true) || contract.status.contains("đã ký", ignoreCase = true) ->
                            Color(0xFFDCFCE7) to Color(0xFF15803D)
                        contract.status.contains("ký", ignoreCase = true) ->
                            Color(0xFFFEF9C3) to Color(0xFFA16207)
                        else ->
                            Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = contract.status,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusTextColor
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Đã chọn",
                    tint = Color(0xFF1D4ED8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
