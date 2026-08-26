@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.ui.theme.SmartContractAITheme
import com.smartcontractai.utils.TemplateVariableParser

data class ChatMessage(
    val sender: String, // "AI" hoặc "User"
    val text: String
)

// ==================== MÀN HÌNH CHỈNH SỬA VĂN BẢN MẪU WORD / DOCS (CÓ SPLIT-SCREEN CHATBOT & CONTEXTUAL REWRITE) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDocumentEditorScreen(
    templateTitle: String = "Hợp Đồng Thử Việc (Bản Chuẩn 2024)",
    onBack: () -> Unit = {},
    onNavigateToDashboard: (Int) -> Unit = {},
    onNavigateToReview: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var bottomNavTab by remember { mutableIntStateOf(1) } // Contracts selected

    // State chế độ Split-Screen Interactive Chatbot
    var isSplitScreenChatbotOpen by remember { mutableStateOf(false) }
    var aiChatInput by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("AI", "Xin chào! Tôi là Trợ lý AI Copilot Split-Screen. Bạn có thể yêu cầu điều chỉnh bất kỳ điều khoản nào (ví dụ: *'Thêm điều khoản phạt chậm thanh toán 0.05%/ngày'*).")
            )
        )
    }

    // State Bôi đen văn bản yêu cầu AI viết lại (Contextual Selection AI Rewrite)
    var isContextualRewriteVisible by remember { mutableStateOf(false) }
    var selectedClauseText by remember { mutableStateOf("ĐIỀU 2: MỨC LƯƠNG VÀ CHẾ ĐỘ THƯỞNG\n2.1. Mức lương thử việc: 15,000,000 VNĐ/tháng.") }

    // Form điền biến mẫu hợp đồng (Fillable Fields)
    var partyBName by remember { mutableStateOf("Nguyễn Văn A") }
    var partyBId by remember { mutableStateOf("012345678901") }
    var positionTitle by remember { mutableStateOf("Chuyên viên Lập trình Android") }
    var salaryAmount by remember { mutableStateOf("15,000,000") }
    var trialDuration by remember { mutableStateOf("02 tháng (Từ 01/09/2024 đến 01/11/2024)") }

    // Nội dung văn bản hợp đồng mẫu Docs/Word
    var contractContent by remember(partyBName, partyBId, positionTitle, salaryAmount, trialDuration) {
        mutableStateOf(
            """
            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
            Độc lập - Tự do - Hạnh phúc
            -------------------

            ${templateTitle.uppercase()}
            Số: 01/2024/HĐTV-AI

            Hôm nay, ngày 01 tháng 09 năm 2024, tại văn phòng trụ sở chính, chúng tôi gồm có:

            BÊN A (BÊN TUYỂN DỤNG): CÔNG TY CỔ PHẦN SMARTCONTRACT AI
            - Đại diện: Ông Nguyễn Quang Minh
            - Chức vụ: Giám đốc Điều hành
            - Mã số thuế: 0312345678
            - Địa chỉ: Tầng 8, Tòa nhà Innovation Center, Quận 1, TP. Hồ Chí Minh

            BÊN B (BÊN NGƯỜI LAO ĐỘNG):
            - Ông/Bà: $partyBName
            - Số CCCD/CMND: $partyBId
            - Chức danh chuyên môn: $positionTitle
            - Địa chỉ thường trú: 123 Đường Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh

            Cùng thỏa thuận ký kết Hợp đồng thử việc với các điều khoản sau đây:

            ĐIỀU 1: CÔNG VIỆC VÀ THỜI HẠN THỬ VIỆC
            1.1. Chức danh công việc: $positionTitle.
            1.2. Thời hạn thử việc: $trialDuration.
            1.3. Địa điểm làm việc: Trụ sở chính Bên A hoặc theo sự phân công hợp lý của Quản lý.

            ĐIỀU 2: MỨC LƯƠNG VÀ CHẾ ĐỘ THƯỞNG
            2.1. Mức lương thử việc: $salaryAmount VNĐ/tháng (Bằng 85% mức lương chính thức).
            2.2. Hình thức trả lương: Chuyển khoản vào tài khoản ngân hàng của Bên B vào ngày 05 hàng tháng.
            2.3. Chế độ đãi ngộ: Được hưởng phụ cấp ăn trưa, gửi xe và tham gia các hoạt động đào tạo của Công ty.

            ĐIỀU 3: QUYỀN VÀ NGHĨA VỤ CỦA CÁC BÊN
            3.1. Bên B có trách nhiệm hoàn thành tốt công việc được giao, chấp hành nội quy lao động của Công ty.
            3.2. Bên A có trách nhiệm thanh toán đầy đủ và đúng hạn các khoản lương, phụ cấp cho Bên B.

            ĐIỀU 4: ĐIỀU KHOẢN THI HÀNH
            Hợp đồng này được lập thành 02 (hai) bản có giá trị pháp lý như nhau, mỗi bên giữ 01 bản.

                      ĐẠI DIỆN BÊN A                                   ĐẠI DIỆN BÊN B
                     (Ký, ghi rõ họ tên)                             (Ký, ghi rõ họ tên)



                      Nguyễn Quang Minh                                $partyBName
            """.trimIndent()
        )
    }

    // Tự động trích xuất các biến {{Var}} bằng TemplateVariableParser
    val dynamicVariables = remember(contractContent) {
        TemplateVariableParser.extractVariables(contractContent)
    }
    var variableValuesMap by remember { mutableStateOf(mutableMapOf<String, String>()) }

    var isEditingFormOpen by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = templateTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (isSplitScreenChatbotOpen) "Bật Chế độ Split-Screen Chatbot AI" else "Định dạng Docs / Word - AI Trợ lý",
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
                    // Nút Chuyển đổi Split-Screen Chatbot AI
                    IconButton(onClick = {
                        isSplitScreenChatbotOpen = !isSplitScreenChatbotOpen
                        Toast.makeText(
                            context,
                            if (isSplitScreenChatbotOpen) "Mở chế độ Split-Screen Chatbot AI" else "Đóng Chatbot Split-Screen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Split Screen AI",
                            tint = if (isSplitScreenChatbotOpen) Color(0xFF7E22CE) else Color(0xFF1D4ED8)
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Đã xuất tệp Word (.docx) thành công!", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Tải file Word",
                            tint = Color(0xFF1D4ED8)
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
                    if (index == 2) {
                        onBack()
                    } else {
                        onNavigateToDashboard(index)
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {

            // ==================== CỬA SỔ CHATBOT AI SPLIT-SCREEN (SONG SONG VĂN BẢN) ====================
            AnimatedVisibility(
                visible = isSplitScreenChatbotOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                    border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Split-Screen AI Header
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
                                        .background(Color(0xFF4F46E5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Interactive AI Copilot (Split-Screen)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF312E81)
                                    )
                                    Text(
                                        text = "Chat để điều chỉnh hợp đồng trực tiếp",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4338CA)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isSplitScreenChatbotOpen = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng Split-Screen",
                                    tint = Color(0xFF4338CA)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Lịch sử Chat với AI Copilot
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(chatMessages) { msg ->
                                    val isAi = msg.sender == "AI"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 10.dp,
                                                        topEnd = 10.dp,
                                                        bottomStart = if (isAi) 2.dp else 10.dp,
                                                        bottomEnd = if (isAi) 10.dp else 2.dp
                                                    )
                                                )
                                                .background(if (isAi) Color(0xFFEEF2FF) else Color(0xFF1D4ED8))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                fontSize = 12.sp,
                                                color = if (isAi) Color(0xFF1E293B) else Color.White,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Khung nhập prompt chỉnh sửa trực tiếp hợp đồng
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aiChatInput,
                                onValueChange = { aiChatInput = it },
                                placeholder = { Text("VD: Thêm điều khoản phạt chậm 0.05%/ngày...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4F46E5),
                                    unfocusedBorderColor = Color(0xFFC7D2FE),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (aiChatInput.isNotBlank()) {
                                        val prompt = aiChatInput.trim()
                                        chatMessages = chatMessages + ChatMessage("User", prompt)
                                        aiChatInput = ""

                                        // AI tự động cập nhật nội dung hợp đồng
                                        contractContent += "\n\nĐIỀU BỔ SUNG (BỞI AI COPILOT):\n- Theo yêu cầu: '$prompt'\n- Các bên cam kết chấp hành bổ sung điều khoản này vào hợp đồng."
                                        chatMessages = chatMessages + ChatMessage("AI", "Đã cập nhật điều khoản mới vào văn bản hợp đồng theo yêu cầu của bạn!")
                                        Toast.makeText(context, "AI đã điều chỉnh văn bản hợp đồng!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF4F46E5))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gửi",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Quick Form Input Panel (Bảng tự động điền biến {{Var}} theo mẫu)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditingFormOpen = !isEditingFormOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Điền nhanh thông tin theo mẫu (Form Variables)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }

                        Icon(
                            imageVector = if (isEditingFormOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }

                    if (isEditingFormOpen) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = partyBName,
                            onValueChange = { partyBName = it },
                            label = { Text("Họ và tên Người lao động (Bên B)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1D4ED8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = partyBId,
                                onValueChange = { partyBId = it },
                                label = { Text("Số CCCD/CMND") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1D4ED8),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            OutlinedTextField(
                                value = salaryAmount,
                                onValueChange = { salaryAmount = it },
                                label = { Text("Mức lương (VNĐ)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1D4ED8),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = positionTitle,
                            onValueChange = { positionTitle = it },
                            label = { Text("Chức danh chuyên môn") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1D4ED8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        // Nếu văn bản có chứa biến động {{...}} phát hiện bằng TemplateVariableParser
                        if (dynamicVariables.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Các biến số phát hiện tự động (${dynamicVariables.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            dynamicVariables.forEach { variable ->
                                val currentVal = variableValuesMap[variable.key] ?: ""
                                OutlinedTextField(
                                    value = currentVal,
                                    onValueChange = { newVal ->
                                        variableValuesMap = variableValuesMap.toMutableMap().apply { put(variable.key, newVal) }
                                        contractContent = TemplateVariableParser.replaceVariables(contractContent, variableValuesMap)
                                    },
                                    label = { Text(variable.displayName) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF7E22CE),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== BAR BÔI ĐEN VĂN BẢN YÊU CẦU AI VIẾT LẠI (CONTEXTUAL SELECTION AI REWRITE) ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                border = BorderStroke(1.dp, Color(0xFFE9D5FF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF7E22CE),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bôi đen bối cảnh - AI Contextual Rewrite",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                        }

                        IconButton(
                            onClick = { isContextualRewriteVisible = !isContextualRewriteVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isContextualRewriteVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF7E22CE)
                            )
                        }
                    }

                    if (isContextualRewriteVisible) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Đoạn văn bản được chọn:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B21A8)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE9D5FF), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = selectedClauseText,
                                fontSize = 11.5.sp,
                                color = Color(0xFF3B0764)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Chọn lệnh cho AI viết lại:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B21A8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val rewritten = "2.1. Lương thử việc: 15,000,000 VNĐ/tháng (85% lương chính thức)."
                                    contractContent = contractContent.replace(selectedClauseText, rewritten)
                                    selectedClauseText = rewritten
                                    Toast.makeText(context, "AI đã viết lại ngắn gọn hơn!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("✂️ Ngắn gọn", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val rewritten = "2.1. Mức lương thử việc: 15,000,000 VNĐ/tháng. Bên B chịu hoàn toàn trách nhiệm nghĩa vụ thuế TNCN và tuân thủ tuyệt đối quy định khấu trừ của Bên A."
                                    contractContent = contractContent.replace(selectedClauseText, rewritten)
                                    selectedClauseText = rewritten
                                    Toast.makeText(context, "AI đã thắt chặt bảo vệ Doanh nghiệp!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("⚖️ Thắt chặt pháp lý", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val rewritten = "2.1. Mức lương thử việc là 15,000,000 VNĐ/tháng. Thanh toán qua tài khoản ngân hàng chính thức vào ngày 05 hàng tháng."
                                    contractContent = contractContent.replace(selectedClauseText, rewritten)
                                    selectedClauseText = rewritten
                                    Toast.makeText(context, "AI đã làm rõ nghĩa điều khoản!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("💡 Làm rõ nghĩa", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Word / Docs Format Editing Toolbar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            Toast.makeText(context, "Đã in đậm (Bold)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("B", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, "Đã in nghiêng (Italic)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("I", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, "Đã gạch chân (Underline)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("U", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                onNavigateToReview(templateTitle, contractContent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Review với AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Đã hoàn tất chỉnh sửa văn bản!", Toast.LENGTH_SHORT).show()
                                onNavigateToReview(templateTitle, contractContent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lưu & Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Word Document Paper View (Trang giấy A4 chứa văn bản hợp đồng trực quan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Trang Word / Docs - Xem & Chỉnh sửa trực tiếp:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = contractContent,
                        onValueChange = { contractContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 500.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContractDocumentEditorScreenPreview() {
    SmartContractAITheme {
        ContractDocumentEditorScreen()
    }
}
