@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.ui.theme.SmartContractAITheme

// ==================== MÀN HÌNH CHỈNH SỬA VĂN BẢN MẪU WORD / DOCS ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDocumentEditorScreen(
    templateTitle: String = "Hợp Đồng Thử Việc (Bản Chuẩn 2024)",
    onBack: () -> Unit = {},
    onNavigateToDashboard: (Int) -> Unit = {},
    onNavigateToReview: (String, String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bottomNavTab by remember { mutableIntStateOf(1) } // Contracts selected

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
                            text = "Định dạng Docs / Word - Người dùng tự điền",
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
                    IconButton(onClick = {
                        Toast.makeText(context, "Đã xuất tệp Word (.docx) thành công!", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Tải file Word",
                            tint = Color(0xFF1D4ED8)
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Đã lưu bản nháp hợp đồng!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Lưu bản nháp",
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

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = trialDuration,
                            onValueChange = { trialDuration = it },
                            label = { Text("Thời hạn thử việc") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1D4ED8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Word / Docs Format Editing Toolbar (Thanh công cụ định dạng Word)
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
