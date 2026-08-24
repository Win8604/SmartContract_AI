@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.data.RecentContractsRepository
import com.smartcontractai.data.UserDatabaseHelper
import com.smartcontractai.data.UserFileManager
import com.smartcontractai.ui.theme.SmartContractAITheme

// ==================== MÀN HÌNH REVIEW & CHỈNH SỬA HỢP ĐỒNG (AI & TRỰC TIẾP) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractReviewScreen(
    contractTitle: String = "Hợp Đồng Dịch Vụ IT & AI Copilot",
    initialContent: String = "",
    source: String = "AI", // "AI" hoặc "Template"
    creatorEmail: String? = null,
    onBack: () -> Unit = {},
    onNavigateToDashboard: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val currentEmail = remember<String> { UserFileManager.getCurrentSessionEmail(context).trim().lowercase() }
    val effectiveCreatorEmail = remember<String>(creatorEmail, currentEmail) {
        val trimmed = creatorEmail?.trim()?.lowercase()
        if (!trimmed.isNullOrBlank()) trimmed else currentEmail.ifEmpty { "nguoidung@smartcontract.ai" }
    }
    val isCreator = remember<Boolean>(currentEmail, creatorEmail) {
        creatorEmail.isNullOrBlank() || currentEmail.isEmpty() || currentEmail == creatorEmail.trim().lowercase()
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Xem Review (Chỉ xem - Mặc định), 1: Tự chỉnh sửa trực tiếp, 2: Chỉnh sửa bằng AI
    var bottomNavTab by remember { mutableIntStateOf(1) } // Contracts selected

    // Nội dung hợp đồng đang review
    val defaultSampleContent = remember<String>(contractTitle, initialContent) {
        initialContent.ifBlank {
            """
            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
            Độc lập - Tự do - Hạnh phúc
            -------------------

            ${contractTitle.uppercase()}
            Mã số tham chiếu: HĐ-2024/SC-AI-99

            Hôm nay, ngày 20 tháng 08 năm 2024, các bên gồm có:

            BÊN A (BÊN GIAO DỊCH / CHỦ ĐẦU TƯ):
            - Tên đơn vị: CÔNG TY CỔ PHẦN CÔNG NGHỆ SMARTCONTRACT AI
            - Đại diện: Ông Nguyễn Quang Minh
            - Chức vụ: Giám đốc Điều hành
            - Mã số thuế: 0312345678
            - Địa chỉ: Tầng 8, Innovation Building, Quận 1, TP. Hồ Chí Minh

            BÊN B (BÊN ĐỐI TÁC / THỰC HIỆN):
            - Tên đơn vị / Cá nhân: CÔNG TY TNHH GIẢI PHÁP PHẦN MỀM TOÀN CẦU
            - Đại diện: Bà Trần Thị Mai
            - Chức vụ: Giám đốc Kỹ thuật
            - Mã số thuế: 0398765432
            - Địa chỉ: Tòa nhà TechPark, Phường Tân Định, Quận 1, TP. Hồ Chí Minh

            Cùng thống nhất nội dung hợp đồng với các điều khoản sau:

            ĐIỀU 1: PHẠM VI CÔNG VIỆC VÀ NỘI DUNG THỰC HIỆN
            1.1. Bên B đảm nhận tư vấn, xây dựng và tích hợp hệ thống Trợ lý Hợp đồng AI Gemini cho Bên A.
            1.2. Tiến độ triển khai: 60 ngày làm việc kể từ ngày bàn giao tài liệu kỹ thuật ban đầu.

            ĐIỀU 2: GIÁ TRỊ HỢP ĐỒNG VÀ PHƯƠNG THỨC THANH TOÁN
            2.1. Tổng giá trị hợp đồng: 350.000.000 VNĐ (Ba trăm năm mươi triệu đồng chẵn - Đã bao gồm VAT).
            2.2. Đợt 1: Thanh toán 40% ngay sau khi ký kết hợp đồng.
            2.3. Đợt 2: Thanh toán 60% còn lại sau khi hoàn tất kiểm thử và nghiệm thu sản phẩm.

            ĐIỀU 3: BẢO MẬT THÔNG TIN VÀ QUYỀN SỞ HỮU TRÍ TUỆ
            3.1. Hai bên cam kết giữ bí mật toàn bộ thông tin mã nguồn, dữ liệu kinh doanh và thuật toán AI.
            3.2. Toàn bộ bản quyền sản phẩm hoàn thiện thuộc sở hữu hoàn toàn của Bên A.

            ĐIỀU 4: PHẠT VI PHẠM VÀ BỒI THƯỜNG THIỆT HẠI
            4.1. Bên vi phạm tiến độ quá 15 ngày phải chịu phạt 0.5% giá trị hợp đồng cho mỗi ngày chậm trễ.
            4.2. Mức phạt tối đa không vượt quá 8% tổng giá trị hợp đồng theo quy định Luật Thương mại.

            ĐẠI DIỆN BÊN A                                     ĐẠI DIỆN BÊN B
           (Ký, ghi rõ họ tên)                                (Ký, ghi rõ họ tên)
            """.trimIndent()
        }
    }

    var contractText by remember { mutableStateOf(defaultSampleContent) }
    var aiPromptText by remember { mutableStateOf("") }
    var isAiProcessing by remember { mutableStateOf(false) }
    var showRiskPanel by remember { mutableStateOf(true) }
    var aiLogMessage by remember { mutableStateOf<String?>(null) }
    var showGenerateQrDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var isSigned by remember { mutableStateOf(false) }

    // Trạng thái định dạng văn bản hợp đồng (Bold, Italic, Underline, Alignment)
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var alignmentState by remember { mutableIntStateOf(0) } // 0: Trái, 1: Giữa, 2: Phải

    val quickAiPrompts = listOf(
        "Bổ sung điều khoản Bảo mật NDA chi tiết",
        "Tăng phạt vi phạm hợp đồng lên 10%",
        "Sửa tiến độ thanh toán thành 3 đợt",
        "Rút gọn ngôn từ pháp lý dễ hiểu"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Review Hợp Đồng",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (source == "AI") "Tạo bởi AI Copilot • Sẵn sàng xem lại" else "Tạo từ Kho Mẫu chuẩn • Đã kiểm duyệt",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                actions = {
                    // Nút Ký xác nhận hợp đồng
                    IconButton(onClick = { showSignatureDialog = true }) {
                        Icon(
                            imageVector = if (isSigned) Icons.Default.Check else Icons.Default.Draw,
                            contentDescription = "Ký xác nhận",
                            tint = if (isSigned) Color(0xFF16A34A) else Color(0xFF2563EB)
                        )
                    }
                    // Nút Tạo mã QR chỉ hiển thị cho Người tạo hợp đồng
                    if (isCreator) {
                        IconButton(onClick = { showGenerateQrDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Tạo mã QR",
                                tint = Color(0xFF7C3AED)
                            )
                        }
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Đang tải file Word (.docx)...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Tải file Word",
                            tint = Color(0xFF1D4ED8)
                        )
                    }
                    IconButton(onClick = {
                        val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                        RecentContractsRepository.addContract(
                            context = context,
                            title = contractTitle,
                            type = if (source == "AI") "AI Generated" else "Template",
                            status = "Đang rà soát",
                            userEmail = sessionEmail
                        )
                        Toast.makeText(context, "Đã lưu hợp đồng vào Cơ sở dữ liệu!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Lưu hợp đồng",
                            tint = Color(0xFF16A34A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = bottomNavTab,
                onTabSelected = { index ->
                    bottomNavTab = index
                    onNavigateToDashboard(index)
                }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {

            // 1. Phân Tích Rủi Ro Pháp Lý bằng AI (AI Risk Badge & Panel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1D4ED8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Đánh giá an toàn AI: 95/100 điểm",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Text(
                                    text = "Hợp đồng tuân thủ Luật Thương mại & Bộ luật Dân sự 2015",
                                    fontSize = 11.sp,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                        }

                        IconButton(onClick = { showRiskPanel = !showRiskPanel }) {
                            Icon(
                                imageVector = if (showRiskPanel) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showRiskPanel) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFDBEAFE))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Khuyên dùng bổ sung: Chưa có điều khoản về trường hợp Bất khả kháng (Force Majeure).",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF92400E),
                                        lineHeight = 16.sp
                                    )
                                    if (isCreator) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "⚡ Nhấn nút 'Chỉnh sửa bằng AI' bên dưới để Gemini tự bổ sung ngay.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8),
                                            modifier = Modifier.clickable {
                                                activeTab = 2
                                                aiPromptText = "Tự động bổ sung điều khoản Bất khả kháng (Force Majeure) vào hợp đồng"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Tab Bar Chuyển Đổi Phương Thức Xem & Chỉnh Sửa Hợp Đồng
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 0: Bản xem Review (Chỉ xem)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 0) Color(0xFF0F172A) else Color.Transparent)
                            .clickable { activeTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Xem Review",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    // Chỉ hiển thị các tab chỉnh sửa nếu người dùng hiện tại là người tạo hợp đồng
                    if (isCreator) {
                        // Tab 1: Người dùng tự chỉnh sửa trực tiếp (Chỉ dành cho người tạo hợp đồng)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 1) Color(0xFF1D4ED8) else Color.Transparent)
                                .clickable { activeTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tự chỉnh sửa trực tiếp",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 1) Color.White else Color(0xFF475569)
                                )
                            }
                        }

                        // Tab 2: Chỉnh sửa bằng Trợ lý AI Gemini (Chỉ dành cho người tạo hợp đồng)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 2) Color(0xFF4F46E5) else Color.Transparent)
                                .clickable { activeTab = 2 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (activeTab == 2) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sửa bằng AI ✨",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 2) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Khung Chỉnh Sửa Theo Tab Được Chọn
            if (activeTab == 2) {
                // GIAO DIỆN CHỈNH SỬA BẰNG AI GEMINI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Yêu cầu Trợ lý AI Gemini sửa đổi hợp đồng",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF312E81)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Prompt Chips
                        Text(
                            text = "Gợi ý câu lệnh sửa nhanh:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickAiPrompts.chunked(2).forEach { rowPrompts: List<String> ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowPrompts.forEach { prompt ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFFEEF2FF))
                                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(20.dp))
                                                .clickable { aiPromptText = prompt }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = prompt,
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF4338CA),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input Box AI Prompt
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = aiPromptText,
                                    onValueChange = { aiPromptText = it },
                                    placeholder = {
                                        Text("Nhập yêu cầu sửa (vd: Thêm điều khoản bảo mật...)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (aiPromptText.isBlank()) {
                                                Toast.makeText(context, "Vui lòng nhập lệnh yêu cầu cho AI", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isAiProcessing = true
                                                aiLogMessage = "AI Gemini đang rà soát và cập nhật điều khoản theo yêu cầu..."
                                                
                                                // Mô phỏng AI cập nhật văn bản hợp đồng thực tế
                                                contractText += "\n\nĐIỀU 5: ĐIỀU KHOẢN BỔ SUNG BỞI AI GEMINI COPILOT\n5.1. Hai bên cam kết thực hiện đúng yêu cầu: $aiPromptText.\n5.2. Các trường hợp bất khả kháng theo quy định pháp luật sẽ được miễn trừ trách nhiệm bồi thường sau khi thông báo bằng văn bản trong vòng 48h."

                                                Toast.makeText(context, "Đã cập nhật hợp đồng bằng AI!", Toast.LENGTH_SHORT).show()
                                                isAiProcessing = false
                                                aiPromptText = ""
                                            }
                                        },
                                        enabled = !isAiProcessing,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isAiProcessing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Đang xử lý...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Gửi",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Sửa bằng AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Nút "Lưu" Hợp đồng vừa sửa bằng AI
                                    Button(
                                        onClick = {
                                            val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                                            RecentContractsRepository.addContract(
                                                context = context,
                                                title = contractTitle,
                                                type = "AI Generated",
                                                status = "Đã chỉnh sửa bởi AI",
                                                userEmail = sessionEmail
                                            )
                                            Toast.makeText(context, "Đã lưu và cập nhật bản xem Review hợp đồng!", Toast.LENGTH_SHORT).show()
                                            activeTab = 0
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Save,
                                                contentDescription = "Lưu",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lưu AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (aiLogMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = aiLogMessage!!,
                                    fontSize = 11.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. Khung Xem & Chỉnh Sửa Văn Bản Hợp Đồng (Thiết kế A4 Docs & Thanh Công Cụ như Hình 2)
            // 4a. Thanh Công Cụ Định Dạng Văn Bản (Rich Word/Docs Toolbar) - Chỉ hiển thị ở tab Tự chỉnh sửa trực tiếp (activeTab == 1)
            if (activeTab == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Cụm định dạng chữ: B, I, U và Căn lề
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // In đậm (Bold)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isBold) Color(0xFFDBEAFE) else Color.Transparent)
                                    .clickable {
                                        isBold = !isBold
                                        Toast.makeText(context, if (isBold) "Đã bật In đậm" else "Đã tắt In đậm", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("B", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (isBold) Color(0xFF1D4ED8) else Color(0xFF0F172A))
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // In nghiêng (Italic)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isItalic) Color(0xFFDBEAFE) else Color.Transparent)
                                    .clickable {
                                        isItalic = !isItalic
                                        Toast.makeText(context, if (isItalic) "Đã bật In nghiêng" else "Đã tắt In nghiêng", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("I", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isItalic) Color(0xFF1D4ED8) else Color(0xFF0F172A), fontStyle = FontStyle.Italic)
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Gạch chân (Underline)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isUnderline) Color(0xFFDBEAFE) else Color.Transparent)
                                    .clickable {
                                        isUnderline = !isUnderline
                                        Toast.makeText(context, if (isUnderline) "Đã bật Gạch chân" else "Đã tắt Gạch chân", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("U", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isUnderline) Color(0xFF1D4ED8) else Color(0xFF0F172A), textDecoration = TextDecoration.Underline)
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))
                            Spacer(modifier = Modifier.width(4.dp))

                            // Căn lề Trái
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (alignmentState == 0) Color(0xFFDBEAFE) else Color.Transparent)
                                    .clickable {
                                        alignmentState = 0
                                        Toast.makeText(context, "Căn lề trái", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("≡", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (alignmentState == 0) Color(0xFF1D4ED8) else Color(0xFF64748B))
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Căn lề Giữa
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (alignmentState == 1) Color(0xFFDBEAFE) else Color.Transparent)
                                    .clickable {
                                        alignmentState = 1
                                        Toast.makeText(context, "Căn lề giữa", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("≡", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (alignmentState == 1) Color(0xFF1D4ED8) else Color(0xFF64748B))
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Căn lề Phải
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (alignmentState == 2) Color(0xFFDBEAFE) else Color.Transparent)
                                    .clickable {
                                        alignmentState = 2
                                        Toast.makeText(context, "Căn lề phải", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("≡", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (alignmentState == 2) Color(0xFF1D4ED8) else Color(0xFF64748B))
                            }
                        }

                        // Cụm Lịch sử phiên bản & Nút Lưu hợp đồng
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { Toast.makeText(context, "Xem lịch sử chỉnh sửa hợp đồng", Toast.LENGTH_SHORT).show() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Lịch sử",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lịch sử", fontSize = 11.sp, color = Color(0xFF64748B))
                            }

                            // Nút "Lưu" hợp đồng đã chỉnh sửa trực tiếp trên thanh công cụ
                            Button(
                                onClick = {
                                    val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                                    RecentContractsRepository.addContract(
                                        context = context,
                                        title = contractTitle,
                                        type = if (source == "AI") "AI Generated" else "Template",
                                        status = "Đã tự chỉnh sửa",
                                        userEmail = sessionEmail
                                    )
                                    Toast.makeText(context, "Đã lưu và cập nhật bản xem Review hợp đồng!", Toast.LENGTH_SHORT).show()
                                    activeTab = 0
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Lưu",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lưu", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4b. Trang Giấy Văn Bản A4 (Có viền vàng trang trí lề trái chuẩn như Hình 2)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // Viền vàng trang trí lề trái (Amber Golden Margin Line như Hình 2)
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF59E0B))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (activeTab == 0) {
                            if (!isCreator) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chế độ Xem Review: Hợp đồng này được tạo bởi $effectiveCreatorEmail. Bạn chỉ có quyền xem review, không thể chỉnh sửa.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bản xem Review (Không cho phép sửa). Bạn là người tạo hợp đồng - Chọn tab 'Tự chỉnh sửa trực tiếp' để sửa.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // TextField nhập/xem nội dung hợp đồng
                        OutlinedTextField(
                            value = contractText,
                            onValueChange = { if (isCreator && activeTab == 1) contractText = it },
                            readOnly = !isCreator || activeTab != 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 480.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isCreator && activeTab == 1) Color(0xFF3B82F6) else Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = if (isCreator && activeTab == 1) Color(0xFFFAFAFA) else Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            textStyle = TextStyle(
                                fontSize = 12.5.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                                textAlign = when (alignmentState) {
                                    1 -> TextAlign.Center
                                    2 -> TextAlign.Right
                                    else -> TextAlign.Left
                                },
                                color = Color(0xFF1E293B)
                            )
                        )

                        if ((activeTab == 1 || activeTab == 2) && isCreator) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                                        RecentContractsRepository.addContract(
                                            context = context,
                                            title = contractTitle,
                                            type = if (activeTab == 2) "AI Generated" else (if (source == "AI") "AI Generated" else "Template"),
                                            status = if (activeTab == 2) "Đã chỉnh sửa bởi AI" else "Đã tự chỉnh sửa",
                                            userEmail = sessionEmail
                                        )
                                        Toast.makeText(context, "Đã lưu và cập nhật bản xem Review hợp đồng!", Toast.LENGTH_SHORT).show()
                                        activeTab = 0
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeTab == 2) "Lưu hợp đồng vừa sửa bằng AI" else "Lưu hợp đồng đã chỉnh sửa",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Nút Hành Động Hoàn Tất / Lưu Hợp Đồng & Ký Tên & Tạo Mã QR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Nút Ký tên xác nhận hợp đồng
                OutlinedButton(
                    onClick = { showSignatureDialog = true },
                    modifier = Modifier.weight(1.05f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSigned) Color(0xFF16A34A) else Color(0xFF2563EB)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isSigned) Color(0xFF16A34A) else Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSigned) Icons.Default.Check else Icons.Default.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isSigned) Color(0xFF16A34A) else Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (isSigned) "Đã ký ✓" else "Ký tên", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Nút Tạo Mã QR - CHỈ HIỂN THỊ CHO NGƯỜI TẠO HỢP ĐỒNG
                if (isCreator) {
                    OutlinedButton(
                        onClick = { showGenerateQrDialog = true },
                        modifier = Modifier.weight(1.05f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C3AED)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF7C3AED)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Tạo mã QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        contractText = defaultSampleContent
                        isSigned = false
                        aiLogMessage = null
                        Toast.makeText(context, "Đã khôi phục hợp đồng ban đầu", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("Khôi phục", fontSize = 11.sp, color = Color(0xFF64748B))
                }

                Button(
                    onClick = {
                        val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                        RecentContractsRepository.addContract(
                            context = context,
                            title = contractTitle,
                            type = if (source == "AI") "AI Generated" else "Template",
                            status = if (isSigned) "Đã ký xác nhận" else "Đã hoàn tất",
                            userEmail = sessionEmail
                        )
                        Toast.makeText(context, "Hoàn tất Review! Hợp đồng đã được lưu vào hệ thống.", Toast.LENGTH_LONG).show()
                        onNavigateToDashboard(1) // Chuyển sang tab Quản lý Hợp đồng
                    },
                    modifier = Modifier.weight(1.35f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Xác nhận & Hoàn tất", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIALOG KÝ XÁC NHẬN HỢP ĐỒNG (MÔ PHỎNG CHUẨN THEO HÌNH ẢNH YÊU CẦU)
    if (showSignatureDialog) {
        val signaturePaths = remember { mutableStateListOf<Path>() }
        var currentPath by remember { mutableStateOf<Path?>(null) }
        var drawPointsCount by remember { mutableIntStateOf(0) }

        Dialog(onDismissRequest = { showSignatureDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tiêu đề & Hướng dẫn
                    Text(
                        text = "Ký xác nhận",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vui lòng ký tên vào khung bên dưới để xác nhận hoàn tất hợp đồng.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Khung Canvas Ký Tên
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (signaturePaths.isEmpty() && currentPath == null) {
                                Text(
                                    text = "Ký tên tại đây",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val newPath = Path().apply {
                                                    moveTo(offset.x, offset.y)
                                                }
                                                currentPath = newPath
                                                drawPointsCount++
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentPath?.lineTo(change.position.x, change.position.y)
                                                drawPointsCount++
                                            },
                                            onDragEnd = {
                                                currentPath?.let { signaturePaths.add(it) }
                                                currentPath = null
                                            },
                                            onDragCancel = {
                                                currentPath = null
                                            }
                                        )
                                    }
                            ) {
                                signaturePaths.forEach { path ->
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF0F172A),
                                        style = Stroke(
                                            width = 4.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                                currentPath?.let { path ->
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF0F172A),
                                        style = Stroke(
                                            width = 4.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nút Xóa chữ ký (Phía dưới góc phải canvas)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                signaturePaths.clear()
                                currentPath = null
                                drawPointsCount = 0
                            },
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧹 Xóa chữ ký",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Khung Thông Báo Pháp Lý (Thẻ xanh lam nhạt)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                        border = BorderStroke(1.dp, Color(0xFFE0F2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Bằng việc ký, bạn đồng ý với các điều khoản của hợp đồng và xác nhận rằng chữ ký điện tử này có giá trị pháp lý tương đương với chữ ký tay của bạn.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E293B),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Nút Chính "Hoàn tất & Gửi hợp đồng"
                    Button(
                        onClick = {
                            if (signaturePaths.isEmpty() && currentPath == null && drawPointsCount == 0) {
                                Toast.makeText(context, "Vui lòng ký tên vào khung trước khi xác nhận!", Toast.LENGTH_SHORT).show()
                            } else {
                                isSigned = true
                                val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                                RecentContractsRepository.addContract(
                                    context = context,
                                    title = contractTitle,
                                    type = if (source == "AI") "AI Generated" else "Template",
                                    status = "Đã ký xác nhận",
                                    userEmail = sessionEmail
                                )
                                Toast.makeText(context, "Đã ký xác nhận hợp đồng thành công!", Toast.LENGTH_LONG).show()
                                showSignatureDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1665D8))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hoàn tất & Gửi hợp đồng",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG TẠO MÃ QR CHỮ KÝ SỐ CERTIFICATE (MÔ PHỎNG CHUẨN THEO HÌNH 2)
    if (showGenerateQrDialog && isCreator) {
        val contractRefId = remember<String>(contractTitle) { "NDA-2024-X1" }
        val qrData = "SMARTCONTRACT_AI|TITLE:$contractTitle|CREATOR:$effectiveCreatorEmail|REF:$contractRefId"
        val encodedQrUrl = remember<String>(qrData) {
            "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${java.net.URLEncoder.encode(qrData, "UTF-8")}"
        }

        Dialog(onDismissRequest = { showGenerateQrDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header với Biểu tượng QR trung tâm
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 6.dp, end = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Mã QR Chữ ký số",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Quét để xác minh mật mã và ký tài liệu $contractRefId.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF334155),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Pill trạng thái "Đã mã hóa & Đang chờ"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFAFAFA))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đã mã hóa & Đang chờ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Khung mã QR trung tâm
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Trang Hiển thị QR - New",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    coil.compose.AsyncImage(
                                        model = encodedQrUrl,
                                        contentDescription = "Mã QR Chữ ký số",
                                        modifier = Modifier.size(170.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Quét mã QR để xác thực tài liệu",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Phiên bản: V1.0.3 | Mã hóa: AES-256",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dòng hướng dẫn & Đếm ngược hiệu lực
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sử dụng Camera hoặc Zalo để quét",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Thanh Nút Thao Tác Dưới Cùng (Tải xuống & Chia sẻ liên kết)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Đang tải xuống mã QR chữ ký số...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tải xuống",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Đã sao chép liên kết mã QR!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1.25f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Chia sẻ liên kết",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContractReviewScreenPreview() {
    SmartContractAITheme {
        ContractReviewScreen()
    }
}
